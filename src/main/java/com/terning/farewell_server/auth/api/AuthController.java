package com.terning.farewell_server.auth.api;

import com.terning.farewell_server.auth.dto.EmailRequest;
import com.terning.farewell_server.auth.application.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-verification-code")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        authService.sendVerificationCode(request.email());
        return ResponseEntity.ok("인증 코드가 성공적으로 발송되었습니다.");
    }
}
