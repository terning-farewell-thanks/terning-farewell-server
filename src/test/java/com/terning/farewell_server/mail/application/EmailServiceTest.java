package com.terning.farewell_server.mail.application;

import com.terning.farewell_server.mail.exception.MailErrorCode;
import com.terning.farewell_server.mail.exception.MailException;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final String TO_EMAIL = "test@example.com";
    private static final String FROM_EMAIL = "noreply@example.com";
    private static final String CODE = "123456";
    private static final String MOCK_HTML_CONTENT = "<html>...</html>";
    private static final String VERIFICATION_EMAIL_SUBJECT = "[터닝] 마지막 선물 신청을 위한 인증 코드입니다.";
    private static final String CONFIRMATION_EMAIL_SUBJECT = "[터닝] 선물 신청이 확정되었습니다.";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
    }

    @Test
    @DisplayName("인증 코드 이메일을 성공적으로 발송해야 한다.")
    void sendVerificationCode_should_send_email_successfully() throws MessagingException, IOException {
        // given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(templateEngine.process(eq("verificationCode"), any(Context.class))).thenReturn(MOCK_HTML_CONTENT);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendVerificationCode(TO_EMAIL, CODE);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(VERIFICATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString()).isEqualTo(TO_EMAIL);
        assertThat(capturedMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
    }

    @Test
    @DisplayName("확정 이메일을 성공적으로 발송해야 한다.")
    void sendConfirmationEmail_should_send_email_successfully() throws MessagingException, IOException {
        // given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(templateEngine.process(eq("confirmationEmail"), any(Context.class))).thenReturn(MOCK_HTML_CONTENT);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendConfirmationEmail(TO_EMAIL);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(CONFIRMATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString()).isEqualTo(TO_EMAIL);
        assertThat(capturedMessage.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
    }

    @Test
    @DisplayName("이메일 발송 실패 시 MailException을 던져야 한다.")
    void sendEmail_should_throw_MailException_on_failure() {
        // given
        when(javaMailSender.createMimeMessage()).thenThrow(new MailException(MailErrorCode.EMAIL_SEND_FAILURE));

        // when & then
        assertThatThrownBy(() -> emailService.sendVerificationCode(TO_EMAIL, CODE))
                .isInstanceOf(MailException.class);
    }
}
