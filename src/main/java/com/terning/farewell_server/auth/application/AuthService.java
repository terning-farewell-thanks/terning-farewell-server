package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.auth.jwt.JwtUtil;
import com.terning.farewell_server.mail.application.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmailService emailService;
    private final VerificationCodeManager verificationCodeManager;
    private final JwtUtil jwtUtil;

    public void sendVerificationCode(String email) {
        String code = verificationCodeManager.issueCode(email);
        emailService.sendVerificationCode(email, code);
    }

    public String verifyEmailCode(String email, String code) {
        verificationCodeManager.verifyCode(email, code);

        return jwtUtil.generateTemporaryToken(email);
    }

    public String getEmailFromToken(String authorizationHeader) {
        String token = jwtUtil.resolveToken(authorizationHeader);
        return jwtUtil.getEmailFromToken(token);
    }
}
