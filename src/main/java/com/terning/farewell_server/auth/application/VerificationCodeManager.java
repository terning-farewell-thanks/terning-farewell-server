package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.auth.exception.AuthErrorCode;
import com.terning.farewell_server.auth.exception.AuthException;
import com.terning.farewell_server.global.common.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class VerificationCodeManager {

    private final RedisService redisService;

    private static final String VERIFICATION_CODE_PREFIX = "verification:";
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(5);

    public String issueCode(String email) {
        String code = createVerificationCode();
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        redisService.setDataWithExpiration(redisKey, code, VERIFICATION_CODE_TTL);
        return code;
    }

    private String createVerificationCode() {
        SecureRandom secureRandom = new SecureRandom();
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    public void verifyCode(String email, String codeToVerify) {
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisService.getData(redisKey);

        if (storedCode == null || !codeToVerify.equals(storedCode)) {
            throw new AuthException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        redisService.deleteData(redisKey);
    }
}
