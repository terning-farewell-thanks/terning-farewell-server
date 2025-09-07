package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.domain.Application;
import com.terning.farewell_server.application.domain.ApplicationRepository;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ApplicationRepository applicationRepository;

    private static final String KAFKA_TOPIC = "event-application";
    private static final String EMAIL = "user@example.com";

    @Test
    @DisplayName("이벤트 신청 시, Kafka로 사용자 이메일 메시지를 정확히 한 번 발행해야 한다.")
    void applyForGift_shouldSendEmailToKafka() {
        // given
        ReflectionTestUtils.setField(eventService, "kafkaTopic", KAFKA_TOPIC);

        // when
        eventService.applyForGift(EMAIL);

        // then
        verify(kafkaTemplate, times(1)).send(KAFKA_TOPIC, EMAIL);
    }

    @Test
    @DisplayName("신청 내역이 존재하는 사용자의 상태 조회를 성공한다.")
    void getApplicationStatus_Success() {
        // given
        Application mockApplication = mock(Application.class);
        when(mockApplication.getStatus()).thenReturn(ApplicationStatus.SUCCESS);
        when(applicationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(mockApplication));

        // when
        StatusResponse response = eventService.getApplicationStatus(EMAIL);

        // then
        assertThat(response.status()).isEqualTo(ApplicationStatus.SUCCESS.name());
    }

    @Test
    @DisplayName("신청 내역이 없는 사용자의 상태 조회 시 예외가 발생한다.")
    void getApplicationStatus_Fail_WhenNotFound() {
        // given
        when(applicationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(EventException.class, () -> eventService.getApplicationStatus(EMAIL));
    }
}

