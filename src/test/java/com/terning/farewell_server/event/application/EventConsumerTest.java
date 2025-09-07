package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.event.exception.EventException;
import com.terning.farewell_server.mail.application.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @InjectMocks
    private EventConsumer eventConsumer;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    private static final String EMAIL = "test@example.com";
    private static final String TOPIC = "event-application";
    private static final String GIFT_STOCK_KEY = "event:gift:stock";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventConsumer, "giftStockKey", GIFT_STOCK_KEY);
    }

    @Test
    @DisplayName("재고가 남아있을 때(Redis decr >= 0), SUCCESS 상태로 저장하고 확인 이메일을 발송한다.")
    void handleApplication_whenStockAvailable_thenProcessSuccess() {
        // given
        when(redisTemplate.execute(any(RedisScript.class), any(List.class)))
                .thenReturn(99L);

        // when
        eventConsumer.handleApplication(EMAIL, TOPIC);

        // then
        InOrder inOrder = inOrder(applicationService, emailService);
        inOrder.verify(applicationService, times(1)).saveApplication(EMAIL, ApplicationStatus.SUCCESS);
        inOrder.verify(emailService, times(1)).sendConfirmationEmail(EMAIL);
    }

    @Test
    @DisplayName("재고가 소진되었을 때(Redis decr < 0), FAILURE 상태로 저장하고 이메일은 발송하지 않는다.")
    void handleApplication_whenStockExhausted_thenProcessFailure() {
        // given
        when(redisTemplate.execute(any(RedisScript.class), any(List.class)))
                .thenReturn(-1L);

        // when
        eventConsumer.handleApplication(EMAIL, TOPIC);

        // then
        verify(applicationService, times(1)).saveApplication(EMAIL, ApplicationStatus.FAILURE);
        verify(emailService, never()).sendConfirmationEmail(EMAIL);
    }

    @Test
    @DisplayName("Redis 스크립트 실행 결과가 null일 때, 재시도를 위해 EventException을 던져야 한다.")
    void handleApplication_whenRedisReturnsNull_thenThrowException() {
        // given
        when(redisTemplate.execute(any(RedisScript.class), any(List.class)))
                .thenReturn(null);

        // when & then
        assertThrows(EventException.class, () -> {
            eventConsumer.handleApplication(EMAIL, TOPIC);
        });

        // then
        verify(applicationService, never()).saveApplication(any(String.class), any(ApplicationStatus.class));
        verify(emailService, never()).sendConfirmationEmail(EMAIL);
    }

    @Test
    @DisplayName("DB 저장 중 예외가 발생하면, 이메일 발송은 시도되지 않고 런타임 예외를 다시 던져야 한다.")
    void handleApplication_whenDbFails_thenThrowRuntimeException() {
        // given
        when(redisTemplate.execute(any(RedisScript.class), any(List.class)))
                .thenReturn(99L);
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(applicationService).saveApplication(EMAIL, ApplicationStatus.SUCCESS);

        // when & then
        assertThrows(RuntimeException.class, () -> {
            eventConsumer.handleApplication(EMAIL, TOPIC);
        });

        // then
        verify(emailService, never()).sendConfirmationEmail(EMAIL);
    }
}
