package com.terning.farewell_server.auth.api;

import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.auth.dto.request.EmailRequest;
import com.terning.farewell_server.auth.dto.request.VerifyCodeRequest;
import com.terning.farewell_server.auth.dto.response.AuthenticationResponse;
import com.terning.farewell_server.global.success.GlobalSuccessCode;
import com.terning.farewell_server.global.success.SuccessResponse;
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
    public ResponseEntity<SuccessResponse<Void>> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        authService.sendVerificationCode(request.email());
        return ResponseEntity.ok(SuccessResponse.from(GlobalSuccessCode.OK));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<SuccessResponse<AuthenticationResponse>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        String token = authService.verifyEmailCode(request.email(), request.code());
        AuthenticationResponse authResponse = new AuthenticationResponse(token);
        return ResponseEntity.ok(SuccessResponse.of(GlobalSuccessCode.OK, authResponse));
    }
}
