package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
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

    private static final String KAFKA_TOPIC = "event-application";

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KAFKA_TOPIC)
    public void handleApplication(String email, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Kafka 메시지 수신 [Topic: {}]: {}", topic, email);
        applicationService.saveApplication(email);
        emailService.sendConfirmationEmail(email);
    }

    @DltHandler
    public void handleDlt(String email, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] 최종 처리 실패 [Topic: {}]: {}", topic, email);
    }
}
