package com.terning.farewell_server.event.application;

import com.terning.farewell_server.event.dto.request.SetStockRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EventAdminService eventAdminService;

    private final String MOCK_GIFT_STOCK_KEY = "event:gift:stock:test";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventAdminService, "giftStockKey", MOCK_GIFT_STOCK_KEY);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("이벤트 재고 설정 요청 시, Redis에 정확한 키와 값으로 저장되어야 한다.")
    void setEventStock_Success() {
        // given
        int stockCount = 100;
        SetStockRequest request = new SetStockRequest(stockCount);

        // when
        int result = eventAdminService.setEventStock(request);

        // then
        verify(valueOperations, times(1)).set(MOCK_GIFT_STOCK_KEY, String.valueOf(stockCount));

        assertThat(result).isEqualTo(stockCount);
    }
}
