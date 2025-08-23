package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.mail.application.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final ApplicationService applicationService;
    private final EmailService emailService;

    private static final String KAFKA_TOPIC = "event-application";

    @KafkaListener(topics = KAFKA_TOPIC)
    public void handleApplication(String email) {
        log.info("Kafka 메시지 수신: {}", email);
        try {
            applicationService.saveApplication(email);

            emailService.sendConfirmationEmail(email);

        } catch (Exception e) {
            log.error("비동기 신청 처리 중 오류 발생: {}", email, e);
        }
    }
}
