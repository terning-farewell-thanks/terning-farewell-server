package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.global.common.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;

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
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean verifyCode(String email, String codeToVerify) {
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisService.getData(redisKey);
        return codeToVerify.equals(storedCode);
    }
}
