package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.mail.application.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmailService emailService;
    private final VerificationCodeManager verificationCodeManager;

    public void sendVerificationCode(String email) {
        String code = verificationCodeManager.issueCode(email);

        emailService.sendVerificationCode(email, code);
    }
}
