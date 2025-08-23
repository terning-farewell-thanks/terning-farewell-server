package com.terning.farewell_server.event.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.event.application.EventFacade;
import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(EventControllerTest.MockConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventFacade eventFacade;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public EventFacade eventFacade() {
            return Mockito.mock(EventFacade.class);
        }

        @Bean
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @Test
    @DisplayName("선물 신청 API 호출에 성공한다.")
    void applyForGift_Success() throws Exception {
        // given
        String token = "Bearer dummy-token";
        String email = "test@example.com";

        when(authService.getEmailFromToken(anyString())).thenReturn(email);
        doNothing().when(eventFacade).applyForGift(email);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/event/apply")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.message").value("신청이 정상적으로 접수되었습니다. 최종 결과는 이메일로 안내됩니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("선착순 마감으로 선물 신청에 실패한다.")
    void applyForGift_Fail_EventClosed() throws Exception {
        // given
        String token = "Bearer dummy-token";
        String email = "test@example.com";

        when(authService.getEmailFromToken(anyString())).thenReturn(email);
        doThrow(new EventException(EventErrorCode.EVENT_CLOSED))
                .when(eventFacade).applyForGift(email);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/event/apply")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("[EVENT ERROR] 아쉽지만 선착순 마감되었습니다."))
                .andDo(print());
    }
}
