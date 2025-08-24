package com.terning.farewell_server.event.api;

import com.terning.farewell_server.auth.jwt.JwtUtil;
import com.terning.farewell_server.event.application.EventService;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.exception.EventErrorCode;
import com.terning.farewell_server.event.exception.EventException;
import com.terning.farewell_server.event.success.EventSuccessCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("선물 신청 API 호출에 성공한다.")
    void applyForGift_Success() throws Exception {
        // given
        String email = "test@example.com";
        doNothing().when(eventService).applyForGift(email);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/event/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
        );

        // then
        resultActions
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(EventSuccessCode.EVENT_APPLICATION_ACCEPTED.getStatus().value()))
                .andExpect(jsonPath("$.message").value(EventSuccessCode.EVENT_APPLICATION_ACCEPTED.getMessage()))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("선착순 마감으로 선물 신청에 실패한다.")
    void applyForGift_Fail_EventClosed() throws Exception {
        // given
        String email = "test@example.com";
        doThrow(new EventException(EventErrorCode.EVENT_CLOSED))
                .when(eventService).applyForGift(email);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/event/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
        );

        // then
        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(EventErrorCode.EVENT_CLOSED.getStatus().value()))
                .andExpect(jsonPath("$.message").value(EventErrorCode.EVENT_CLOSED.getMessage()))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("신청 상태 조회 API 호출에 성공한다.")
    void getApplicationStatus_Success() throws Exception {
        // given
        StatusResponse mockResponse = new StatusResponse("ACCEPTED", "신청 결과: ACCEPTED");
        when(eventService.getApplicationStatus(anyString())).thenReturn(mockResponse);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/event/status"));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EventSuccessCode.GET_EVENT_STATUS_SUCCESS.getStatus().value()))
                .andExpect(jsonPath("$.message").value(EventSuccessCode.GET_EVENT_STATUS_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.result.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.result.message").value("신청 결과: ACCEPTED"))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("신청 내역이 없을 때 상태 조회 API 호출에 실패한다.")
    void getApplicationStatus_Fail_WhenNotFound() throws Exception {
        // given
        when(eventService.getApplicationStatus(anyString()))
                .thenThrow(new EventException(EventErrorCode.APPLICATION_NOT_FOUND));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/event/status"));

        // then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(EventErrorCode.APPLICATION_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.message").value(EventErrorCode.APPLICATION_NOT_FOUND.getMessage()))
                .andDo(print());
    }
}
