package com.terning.farewell_server.auth.jwt;

import com.terning.farewell_server.auth.exception.AuthErrorCode;
import com.terning.farewell_server.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET_KEY = "dGhpc2lzYW5leGFtcGxlb2Zhc2VjdXJlYW5kcmFuZG9tbHlnZW5lcmF0ZWRzZWNyZXRrZXlmb3Jqd3Q=";
    private static final long ONE_HOUR_EXPIRATION = 3600000L;
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", ONE_HOUR_EXPIRATION);
        jwtUtil.init();
    }

    @Test
    @DisplayName("토큰 생성 및 파싱 성공 테스트")
    void generateAndParseToken_Success() {
        // given
        String token = jwtUtil.generateTemporaryToken(EMAIL);

        // when
        Claims claims = jwtUtil.parseToken(token);

        // then
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("만료된 토큰 파싱 시 AuthException(EXPIRED_JWT_TOKEN) 발생")
    void parseToken_Fail_Expired() {
        // given
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 0L);
        jwtUtil.init();
        String expiredToken = jwtUtil.generateTemporaryToken(EMAIL);

        // when & then
        assertThatThrownBy(() -> jwtUtil.parseToken(expiredToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.EXPIRED_JWT_TOKEN.getMessage());
    }

    @Test
    @DisplayName("잘못된 서명을 가진 토큰 파싱 시 AuthException(INVALID_JWT_SIGNATURE) 발생")
    void parseToken_Fail_InvalidSignature() {
        // given
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secretKeyString", "another-secret-key-string-for-testing-purpose-only-!@#$");
        ReflectionTestUtils.setField(otherJwtUtil, "expirationTime", ONE_HOUR_EXPIRATION);
        otherJwtUtil.init();

        String tokenWithWrongSignature = otherJwtUtil.generateTemporaryToken(EMAIL);

        // when & then
        assertThatThrownBy(() -> jwtUtil.parseToken(tokenWithWrongSignature))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_JWT_SIGNATURE.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 형식의 토큰 파싱 시 AuthException(MALFORMED_JWT_TOKEN) 발생")
    void parseToken_Fail_InvalidFormat() {
        // given
        String invalidToken = "this.is.not.a.valid.jwt.token";

        // when & then
        assertThatThrownBy(() -> jwtUtil.parseToken(invalidToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.MALFORMED_JWT_TOKEN.getMessage());
    }

    @Test
    @DisplayName("'Bearer ' 접두사가 있는 헤더에서 토큰을 성공적으로 추출한다.")
    void resolveToken_Success() {
        // given
        String rawToken = "raw.jwt.token";
        String bearerToken = "Bearer " + rawToken;

        // when
        String resolvedToken = jwtUtil.resolveToken(bearerToken);

        // then
        assertThat(resolvedToken).isEqualTo(rawToken);
    }

    @ParameterizedTest
    @DisplayName("'Bearer ' 접두사가 없거나, null 또는 빈 문자열일 경우 AuthException(INVALID_JWT_TOKEN) 발생")
    @NullAndEmptySource
    @ValueSource(strings = {"InvalidToken", " Bearer token"})
    void resolveToken_Fail_InvalidFormat(String invalidToken) {
        // when & then
        assertThatThrownBy(() -> jwtUtil.resolveToken(invalidToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_JWT_TOKEN.getMessage());
    }

    @Test
    @DisplayName("유효한 토큰에서 이메일을 성공적으로 추출한다.")
    void getEmailFromToken_Success() {
        // given
        String token = jwtUtil.generateTemporaryToken(EMAIL);

        // when
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // then
        assertThat(extractedEmail).isEqualTo(EMAIL);
    }
}
