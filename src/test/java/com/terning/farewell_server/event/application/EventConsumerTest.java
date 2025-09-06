package com.terning.farewell_server.event.application;

import com.terning.farewell_server.application.application.ApplicationService;
import com.terning.farewell_server.application.domain.ApplicationStatus;
import com.terning.farewell_server.mail.application.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventConsumer eventConsumer;

    private static final String EMAIL = "test@example.com";
    private static final String TOPIC = "event-application";

    @Test
    @DisplayName("SUCCESS 상태의 Kafka 메시지 수신 시, 신청 내역 저장과 이메일 발송이 순서대로 호출된다.")
    void handleApplication_Success() {
        // given
        String successMessage = EMAIL + ":" + ApplicationStatus.SUCCESS.name();
        doNothing().when(applicationService).saveApplication(EMAIL, ApplicationStatus.SUCCESS);
        doNothing().when(emailService).sendConfirmationEmail(EMAIL);

        // when
        eventConsumer.handleApplication(successMessage, TOPIC);

        // then
        InOrder inOrder = inOrder(applicationService, emailService);
        inOrder.verify(applicationService, times(1)).saveApplication(EMAIL, ApplicationStatus.SUCCESS);
        inOrder.verify(emailService, times(1)).sendConfirmationEmail(EMAIL);
    }

    @Test
    @DisplayName("FAILURE 상태의 Kafka 메시지 수신 시, 신청 내역만 저장되고 이메일 발송은 호출되지 않는다.")
    void handleApplication_Failure_Status() {
        // given
        String failureMessage = EMAIL + ":" + ApplicationStatus.FAILURE.name();
        doNothing().when(applicationService).saveApplication(EMAIL, ApplicationStatus.FAILURE);

        // when
        eventConsumer.handleApplication(failureMessage, TOPIC);

        // then
        verify(applicationService, times(1)).saveApplication(EMAIL, ApplicationStatus.FAILURE);
        verify(emailService, never()).sendConfirmationEmail(anyString());
    }

    @Test
    @DisplayName("신청 내역 저장 중 예외 발생 시, 런타임 예외를 다시 던져야 한다.")
    void handleApplication_Fail_On_Save() {
        // given
        String successMessage = EMAIL + ":" + ApplicationStatus.SUCCESS.name();
        doThrow(new RuntimeException("DB 저장 실패"))
                .when(applicationService).saveApplication(EMAIL, ApplicationStatus.SUCCESS);

        // when & then
        assertThrows(RuntimeException.class, () -> {
            eventConsumer.handleApplication(successMessage, TOPIC);
        });

        // then
        verify(applicationService, times(1)).saveApplication(EMAIL, ApplicationStatus.SUCCESS);
        verify(emailService, never()).sendConfirmationEmail(anyString());
    }
}
