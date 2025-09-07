package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationRepository applicationRepository;

    @Value("${event.kafka-topic}")
    private String kafkaTopic;

    public void applyForGift(String email) {
        log.info("이벤트 신청 접수. Kafka 토픽으로 메시지 발행: {}", email);
        kafkaTemplate.send(kafkaTopic, email);
    }

    @Transactional(readOnly = true)
    public StatusResponse getApplicationStatus(String email) {
        Application application = applicationRepository.findByEmail(email)
                .orElseThrow(() -> new EventException(EventErrorCode.APPLICATION_NOT_FOUND));
        return StatusResponse.from(application);
    }
}
