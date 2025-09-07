package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.mail.application.EmailService;
import datadog.trace.api.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final ApplicationService applicationService;
    private final EmailService emailService;

    @Trace(operationName = "kafka.consume", resourceName = "EventConsumer.handleApplication")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "${event.kafka-topic}")
    public void handleApplication(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        String[] parts = message.split(":");

        if (parts.length != 2) {
            log.error("잘못된 형식의 Kafka 메시지 수신 [Topic: {}]: '{}'. 'email:STATUS' 형식을 따라야 합니다.", topic, message);
            return;
        }

        String email = parts[0];
        ApplicationStatus status;

        try {
            status = ApplicationStatus.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 ApplicationStatus 값 수신 [Topic: {}]: '{}' in message '{}'", topic, parts[1], message);
            return;
        }

        try {
            log.info("Kafka 메시지 수신 [Topic: {}]: Email={}, Status={}", topic, email, status);

            applicationService.saveApplication(email, status);

            if (status == ApplicationStatus.SUCCESS) {
                emailService.sendConfirmationEmail(email);
            }

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 비즈니스 로직 오류 발생: {}", message, e);
            throw new RuntimeException("Kafka message processing failed for message: " + message, e);
        }
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] 최종 처리 실패 [Topic: {}]: {}", topic, message);
    }
}
