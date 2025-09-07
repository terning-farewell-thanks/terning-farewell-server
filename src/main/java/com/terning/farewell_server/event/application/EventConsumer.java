package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.mail.application.EmailService;
import datadog.trace.api.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final ApplicationService applicationService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${event.gift-stock-key}")
    private String giftStockKey;

    private static final String DECREMENT_STOCK_LUA_SCRIPT =
            "local stock = redis.call('decr', KEYS[1]) " +
                    "if tonumber(stock) < 0 then " +
                    "  redis.call('incr', KEYS[1]) " +
                    "  return -1 " +
                    "end " +
                    "return stock";

    private static final RedisScript<Long> DECREMENT_STOCK_SCRIPT =
            new DefaultRedisScript<>(DECREMENT_STOCK_LUA_SCRIPT, Long.class);


    @Trace(operationName = "kafka.consume", resourceName = "EventConsumer.handleApplication")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "${event.kafka-topic}")
    public void handleApplication(String email, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Kafka 메시지 수신 [Topic: {}]: Email={}", topic, email);

        try {
            Long remainingStock = redisTemplate.execute(DECREMENT_STOCK_SCRIPT, Collections.singletonList(giftStockKey));

            if (remainingStock == null || remainingStock < 0) {
                log.info("선착순 마감. [사용자: {}]", email);
                applicationService.saveApplication(email, ApplicationStatus.FAILURE);
                return;
            }

            log.info("선착순 통과! [사용자: {}, 남은 재고: {}]", email, remainingStock);
            applicationService.saveApplication(email, ApplicationStatus.SUCCESS);
            emailService.sendConfirmationEmail(email);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 비즈니스 로직 오류 발생: {}", email, e);
            throw new RuntimeException("Kafka message processing failed for message: " + email, e);
        }
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] 최종 처리 실패 [Topic: {}]: {}", topic, message);
    }
}

