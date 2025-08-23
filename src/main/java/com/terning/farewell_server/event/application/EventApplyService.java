package com.terning.farewell_server.event.application;

import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventApplyService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    @Value("${event.kafka-topic}")
    private String kafkaTopic;

    public void applyForGift(String email) {
        String lockKey = "lock:event:apply:" + email;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(10, 1, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("락 획득 실패: {}", email);
                throw new EventException(EventErrorCode.ALREADY_PROCESSING_REQUEST);
            }

            Long stock = redisTemplate.opsForValue().decrement(giftStockKey);

            if (stock == null) {
                throw new EventException(EventErrorCode.GIFT_STOCK_INFO_NOT_FOUND);
            }

            if (stock < 0) {
                redisTemplate.opsForValue().increment(giftStockKey);
                log.info("선착순 마감되었습니다. [사용자: {}]", email);
                throw new EventException(EventErrorCode.EVENT_CLOSED);
            }

            log.info("선착순 통과! [사용자: {}, 남은 재고: {}]", email, stock);
            kafkaTemplate.send(kafkaTopic, email);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventException(EventErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
