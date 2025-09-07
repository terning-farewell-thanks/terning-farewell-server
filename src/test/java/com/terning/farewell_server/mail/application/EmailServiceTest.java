package com.terning.farewell_server.mail.application;

import com.terning.farewell_server.mail.exception.MailErrorCode;
import com.terning.farewell_server.mail.exception.MailException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    private static final String FROM_EMAIL = "noreply@example.com";
    private static final String MOCK_HTML_CONTENT = "<html>...</html>";
    private static final String VERIFICATION_EMAIL_SUBJECT = "[터닝] 마지막 선물 신청을 위한 인증 코드입니다.";
    private static final String CONFIRMATION_EMAIL_SUBJECT = "[터닝] 선물 신청이 확정되었습니다.";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(MOCK_HTML_CONTENT);
    }

    @Test
    @DisplayName("인증 코드 이메일을 성공적으로 발송해야 한다.")
    void sendVerificationCode_should_send_email_successfully() throws MessagingException {
        // given
        String toEmail = "test@example.com";
        String code = "123456";
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        // when
        emailService.sendVerificationCode(toEmail, code);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(VERIFICATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(toEmail);
        assertThat(capturedMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
    }

    @Test
    @DisplayName("확정 이메일 발송 시 @example.com 이메일은 SES 시뮬레이터 주소로 변경되어야 한다.")
    void sendConfirmationEmail_withTestEmail_shouldChangeToSimulatorAddress() throws MessagingException {
        // given
        String testEmail = "test@example.com";
        String expectedTargetEmail = "test+success@simulator.amazonses.com";
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        // when
        emailService.sendConfirmationEmail(testEmail);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(CONFIRMATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(expectedTargetEmail);
        assertThat(capturedMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
    }

    @Test
    @DisplayName("확정 이메일 발송 시 일반 이메일은 원래 주소 그대로 발송되어야 한다.")
    void sendConfirmationEmail_withRealEmail_shouldKeepOriginalAddress() throws MessagingException {
        // given
        String realEmail = "user@realdomain.com";
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        // when
        emailService.sendConfirmationEmail(realEmail);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(CONFIRMATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(realEmail);
        assertThat(capturedMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
    }

    @Test
    @DisplayName("확정 이메일 발송 시 @가 없는 이메일은 변환 없이 그대로 보내져야 한다.")
    void sendConfirmationEmail_withInvalidEmail_shouldNotTransform() throws MessagingException {
        // given
        String invalidEmail = "invalid-email";
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        // when
        emailService.sendConfirmationEmail(invalidEmail);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();
        assertThat(capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(invalidEmail);
    }

    @Test
    @DisplayName("MimeMessageHelper 작업 실패 시 커스텀 MailException을 던져야 한다.")
    void sendEmail_should_throw_CustomMailException_when_MimeHelperFails() throws MessagingException {
        // given
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doThrow(new MessagingException("Simulated MimeMessage failure"))
                .when(mockMimeMessage).setFrom(any(Address.class));

        // when & then
        assertThatThrownBy(() -> emailService.sendVerificationCode("fail@example.com", "123"))
                .isInstanceOf(MailException.class)
                .hasFieldOrPropertyWithValue("errorCode", MailErrorCode.EMAIL_SEND_FAILURE);
    }

    @Test
    @DisplayName("JavaMailSender.send() 실패 시 커스텀 MailException을 던져야 한다.")
    void sendEmail_should_throw_CustomMailException_when_SenderFails() {
        // given
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("Simulated sender failure"))
                .when(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailService.sendVerificationCode("fail@example.com", "123"))
                .isInstanceOf(MailException.class)
                .hasFieldOrPropertyWithValue("errorCode", MailErrorCode.EMAIL_SEND_FAILURE);
    }
}
