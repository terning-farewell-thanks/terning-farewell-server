package com.terning.farewell_server.mail.application;

import com.terning.farewell_server.mail.exception.MailErrorCode;
import com.terning.farewell_server.mail.exception.MailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    private static final String VERIFICATION_EMAIL_SUBJECT = "[터닝] 마지막 선물 신청을 위한 인증 코드입니다.";

    public void sendVerificationCode(String toEmail, String code) {
        Context context = new Context();
        context.setVariable("code", code);

        String htmlContent = templateEngine.process("verificationCode", context);

        sendEmail(toEmail, VERIFICATION_EMAIL_SUBJECT, htmlContent);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(htmlBody, true);

            javaMailSender.send(mimeMessage);
            log.info("이메일 발송 성공! 수신자: {}, 제목: {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패. 수신자: {}", toEmail, e);
            throw new MailException(MailErrorCode.EMAIL_SEND_FAILURE);
        }
    }
}
