package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final String LOCK_KEY_PREFIX = "lock:event:apply:";
    private static final long LOCK_WAIT_TIME_SECONDS = 10L;
    private static final long LOCK_LEASE_TIME_SECONDS = 1L;

    private static final String LOG_LOCK_ACQUISITION_FAILED = "락 획득 실패: {}";
    private static final String LOG_EVENT_CLOSED = "선착순 마감되었습니다. [사용자: {}]";
    private static final String LOG_EVENT_PASSED = "선착순 통과! [사용자: {}, 남은 재고: {}]";

    private static final String DECREMENT_STOCK_LUA_SCRIPT =
            "local stock = redis.call('decr', KEYS[1]) " +
                    "if tonumber(stock) < 0 then " +
                    "  redis.call('incr', KEYS[1]) " +
                    "  return -1 " +
                    "end " +
                    "return stock";

    private static final RedisScript<Long> DECREMENT_STOCK_SCRIPT =
            new DefaultRedisScript<>(DECREMENT_STOCK_LUA_SCRIPT, Long.class);


    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationRepository applicationRepository;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    @Value("${event.kafka-topic}")
    private String kafkaTopic;


    public void applyForGift(String email) {
        final String lockKey = LOCK_KEY_PREFIX + email;
        final RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn(LOG_LOCK_ACQUISITION_FAILED, email);
                throw new EventException(EventErrorCode.ALREADY_PROCESSING_REQUEST);
            }

            Long remainingStock = redisTemplate.execute(DECREMENT_STOCK_SCRIPT, Collections.singletonList(giftStockKey));

            if (remainingStock == null) {
                throw new EventException(EventErrorCode.GIFT_STOCK_INFO_NOT_FOUND);
            }

            if (remainingStock < 0) {
                log.info(LOG_EVENT_CLOSED, email);
                throw new EventException(EventErrorCode.EVENT_CLOSED);
            }

            log.info(LOG_EVENT_PASSED, email, remainingStock);
            kafkaTemplate.send(kafkaTopic, email);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventException(EventErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public StatusResponse getApplicationStatus(String email) {
        Application application = applicationRepository.findByEmail(email)
                .orElseThrow(() -> new EventException(EventErrorCode.APPLICATION_NOT_FOUND));

        return StatusResponse.from(application);
    }
}
