package com.terning.farewell_server.auth.application;

import com.terning.farewell_server.mail.application.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private VerificationCodeManager verificationCodeManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "test@example.com";
    private static final String MOCK_CODE = "123456";

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
}
