package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.auth.exception.AuthErrorCode;
import com.terning.farewell_server.auth.exception.AuthException;
import com.terning.farewell_server.auth.jwt.JwtUtil;
import com.terning.farewell_server.mail.application.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private VerificationCodeManager verificationCodeManager;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "test@example.com";
    private static final String MOCK_CODE = "123456";
    private static final String MOCK_TOKEN = "mock.jwt.token";

    @Test
    @DisplayName("인증 코드 발송 요청 시, 매니저와 이메일 서비스가 올바른 순서로 호출되어야 한다.")
    void sendVerificationCode_should_call_dependencies_in_correct_order() {
        // given
        when(verificationCodeManager.issueCode(EMAIL)).thenReturn(MOCK_CODE);

        doNothing().when(emailService).sendVerificationCode(EMAIL, MOCK_CODE);

        // when
        authService.sendVerificationCode(EMAIL);

        // then
        verify(verificationCodeManager, times(1)).issueCode(EMAIL);
        verify(emailService, times(1)).sendVerificationCode(EMAIL, MOCK_CODE);

        InOrder inOrder = inOrder(verificationCodeManager, emailService);
        inOrder.verify(verificationCodeManager).issueCode(EMAIL);
        inOrder.verify(emailService).sendVerificationCode(EMAIL, MOCK_CODE);
    }

    @Test
    @DisplayName("인증 코드 검증 성공 시, 임시 토큰을 반환한다.")
    void verifyEmailCode_Success() {
        // given
        doNothing().when(verificationCodeManager).verifyCode(EMAIL, MOCK_CODE);
        when(jwtUtil.generateTemporaryToken(EMAIL)).thenReturn(MOCK_TOKEN);

        // when
        String resultToken = authService.verifyEmailCode(EMAIL, MOCK_CODE);

        // then
        assertThat(resultToken).isEqualTo(MOCK_TOKEN);
        verify(verificationCodeManager, times(1)).verifyCode(EMAIL, MOCK_CODE);
        verify(jwtUtil, times(1)).generateTemporaryToken(EMAIL);
    }

    @Test
    @DisplayName("인증 코드 검증 실패 시, AuthException을 발생시킨다.")
    void verifyEmailCode_Fail() {
        // given
        doThrow(new AuthException(AuthErrorCode.INVALID_VERIFICATION_CODE))
                .when(verificationCodeManager).verifyCode(EMAIL, MOCK_CODE);

        // when & then
        assertThatThrownBy(() -> authService.verifyEmailCode(EMAIL, MOCK_CODE))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_VERIFICATION_CODE.getMessage());

        verify(jwtUtil, never()).generateTemporaryToken(anyString());
    }
}
