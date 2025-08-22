package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.global.common.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeManagerTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private VerificationCodeManager verificationCodeManager;

    private static final String EMAIL = "test@example.com";
    private static final String VERIFICATION_CODE_PREFIX = "verification:";
    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(5);

    @Test
    @DisplayName("인증 코드 발급 시 Redis에 올바른 값으로 저장되어야 한다")
    void issueCode_should_save_code_to_redis() {
        // given

        // when
        String code = verificationCodeManager.issueCode(EMAIL);

        // then
        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}");

        verify(redisService, times(1)).setDataWithExpiration(
                VERIFICATION_CODE_PREFIX + EMAIL,
                code,
                VERIFICATION_CODE_TTL
        );
    }

    @Test
    @DisplayName("유효한 코드로 검증 요청 시 true를 반환해야 한다")
    void verifyCode_with_valid_code_should_return_true() {
        // given
        String validCode = "123456";
        when(redisService.getData(VERIFICATION_CODE_PREFIX + EMAIL)).thenReturn(validCode);

        // when
        boolean isValid = verificationCodeManager.verifyCode(EMAIL, validCode);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 코드로 검증 요청 시 false를 반환해야 한다")
    void verifyCode_with_invalid_code_should_return_false() {
        // given
        String storedCode = "123456";
        String invalidCode = "654321";
        when(redisService.getData(VERIFICATION_CODE_PREFIX + EMAIL)).thenReturn(storedCode);

        // when
        boolean isValid = verificationCodeManager.verifyCode(EMAIL, invalidCode);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Redis에 코드가 없는 경우 false를 반환해야 한다")
    void verifyCode_with_no_code_in_redis_should_return_false() {
        // given
        when(redisService.getData(VERIFICATION_CODE_PREFIX + EMAIL)).thenReturn(null);
        String codeToVerify = "123456";

        // when
        boolean isValid = verificationCodeManager.verifyCode(EMAIL, codeToVerify);

        // then
        assertThat(isValid).isFalse();
    }
}
