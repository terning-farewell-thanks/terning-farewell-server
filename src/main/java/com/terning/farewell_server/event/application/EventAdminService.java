package com.terning.farewell_server.event.application;

import com.terning.farewell_server.event.dto.request.SetStockRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAdminService {

    private final StringRedisTemplate redisTemplate;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    public int setEventStock(SetStockRequest request) {
        int count = request.count();
        redisTemplate.opsForValue().set(giftStockKey, String.valueOf(count));
        log.info("이벤트 재고가 관리자에 의해 설정되었습니다. 총 재고: {}개", count);
        return count;
    }
}
