package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.mail.application.EmailService;
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

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "${event.kafka-topic}")
    public void handleApplication(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            String[] parts = message.split(":");
            String email = parts[0];
            ApplicationStatus status = ApplicationStatus.valueOf(parts[1]);

            log.info("Kafka 메시지 수신 [Topic: {}]: Email={}, Status={}", topic, email, status);

            applicationService.saveApplication(email, status);

            if (status == ApplicationStatus.SUCCESS) {
                emailService.sendConfirmationEmail(email);
            }

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 오류 발생: {}", message, e);
            throw new RuntimeException("Kafka message processing failed for message: " + message, e);
        }
    }

    @DltHandler
    public void handleDlt(String email, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] 최종 처리 실패 [Topic: {}]: {}", topic, email);
    }
}
