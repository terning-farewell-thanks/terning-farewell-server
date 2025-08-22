package com.terning.farewell_server.mail.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private static final String CODE = "123456";
    private static final String MOCK_HTML_CONTENT = "<html>...</html>";
    private static final String VERIFICATION_EMAIL_SUBJECT = "[터닝] 마지막 선물 신청을 위한 인증 코드입니다.";

    @Test
    @DisplayName("유효한 이메일과 코드로 이메일 전송을 요청하면 성공해야 한다.")
    void sendVerificationCode_should_send_email_successfully() throws MessagingException {
        // given
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);

        when(templateEngine.process(eq("verificationCode"), any(Context.class))).thenReturn(MOCK_HTML_CONTENT);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendVerificationCode(TO_EMAIL, CODE);

        // then
        verify(javaMailSender).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertThat(capturedMessage.getSubject()).isEqualTo(VERIFICATION_EMAIL_SUBJECT);
        assertThat(capturedMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString()).isEqualTo(TO_EMAIL);
    }

    @Test
    @DisplayName("이메일 발송 실패 시 RuntimeException을 던져야 한다.")
    void sendVerificationCode_should_throw_exception_on_failure() {
        // given
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);

        when(templateEngine.process(eq("verificationCode"), any(Context.class))).thenReturn(MOCK_HTML_CONTENT);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Failed to send email")).when(javaMailSender).send(any(MimeMessage.class));

        // when & then
        assertThrows(RuntimeException.class, () -> emailService.sendVerificationCode(TO_EMAIL, CODE));
    }
}
