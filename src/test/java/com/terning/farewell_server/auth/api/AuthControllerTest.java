package com.terning.farewell_server.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.auth.dto.EmailRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("유효한 이메일로 인증 코드 발송을 요청하면 성공(200 OK)한다.")
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
                .andExpect(content().string("인증 코드가 성공적으로 발송되었습니다."));

        verify(authService).sendVerificationCode("test@example.com");
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 요청하면 실패(400 Bad Request)한다.")
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
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일을 비워서 요청하면 실패(400 Bad Request)한다.")
    void sendVerificationCode_Fail_BlankEmail() throws Exception {
        // given
        EmailRequest request = new EmailRequest("");
        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest());
    }
}
