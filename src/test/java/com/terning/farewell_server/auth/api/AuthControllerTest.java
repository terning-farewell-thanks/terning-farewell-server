package com.terning.farewell_server.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.auth.dto.request.EmailRequest;
import com.terning.farewell_server.auth.dto.request.VerifyCodeRequest;
import com.terning.farewell_server.auth.exception.AuthErrorCode;
import com.terning.farewell_server.auth.exception.AuthException;
import com.terning.farewell_server.global.error.GlobalErrorCode;
import com.terning.farewell_server.global.success.GlobalSuccessCode;
import com.terning.farewell_server.mail.exception.MailErrorCode;
import com.terning.farewell_server.mail.exception.MailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.MockConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @Test
    @DisplayName("유효한 이메일로 인증 코드 발송을 요청하면 표준 성공 응답(200 OK)을 반환한다.")
    void sendVerificationCode_Success() throws Exception {
        // given
        EmailRequest request = new EmailRequest("test@example.com");
        String requestBody = objectMapper.writeValueAsString(request);
        doNothing().when(authService).sendVerificationCode("test@example.com");

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(GlobalSuccessCode.OK.getStatus().value()))
                .andExpect(jsonPath("$.message").value(GlobalSuccessCode.OK.getMessage()));
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 요청하면 표준 실패 응답(400 Bad Request)을 반환한다.")
    void sendVerificationCode_Fail_InvalidEmail() throws Exception {
        // given
        EmailRequest request = new EmailRequest("invalid-email");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(GlobalErrorCode.INVALID_INPUT_VALUE.getStatus().value()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("이메일 발송 중 서버 오류 발생 시 표준 실패 응답(500 Internal Server Error)을 반환한다.")
    void sendVerificationCode_Fail_ServiceError() throws Exception {
        // given
        EmailRequest request = new EmailRequest("test@example.com");
        String requestBody = objectMapper.writeValueAsString(request);

        doThrow(new MailException(MailErrorCode.EMAIL_SEND_FAILURE))
                .when(authService).sendVerificationCode("test@example.com");

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(MailErrorCode.EMAIL_SEND_FAILURE.getStatus().value()))
                .andExpect(jsonPath("$.message").value(MailErrorCode.EMAIL_SEND_FAILURE.getMessage()));
    }

    @Test
    @DisplayName("유효한 이메일과 코드로 검증 요청 시, 토큰과 함께 표준 성공 응답(200 OK)을 반환한다.")
    void verifyCode_Success() throws Exception {
        // given
        VerifyCodeRequest request = new VerifyCodeRequest("test@example.com", "123456");
        String requestBody = objectMapper.writeValueAsString(request);
        String mockToken = "mock.jwt.token";

        when(authService.verifyEmailCode("test@example.com", "123456")).thenReturn(mockToken);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(GlobalSuccessCode.OK.getStatus().value()))
                .andExpect(jsonPath("$.message").value(GlobalSuccessCode.OK.getMessage()))
                .andExpect(jsonPath("$.result.temporaryToken").value(mockToken));
    }

    @Test
    @DisplayName("유효하지 않은 코드로 검증 요청 시, 표준 실패 응답(400 Bad Request)을 반환한다.")
    void verifyCode_Fail_InvalidCode() throws Exception {
        // given
        VerifyCodeRequest request = new VerifyCodeRequest("test@example.com", "999999");
        String requestBody = objectMapper.writeValueAsString(request);

        when(authService.verifyEmailCode("test@example.com", "999999"))
                .thenThrow(new AuthException(AuthErrorCode.INVALID_VERIFICATION_CODE));

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(AuthErrorCode.INVALID_VERIFICATION_CODE.getStatus().value()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_VERIFICATION_CODE.getMessage()));
    }
}
