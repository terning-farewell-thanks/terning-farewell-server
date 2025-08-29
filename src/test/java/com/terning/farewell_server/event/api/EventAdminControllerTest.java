package com.terning.farewell_server.event.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terning.farewell_server.event.application.EventAdminService;
import com.terning.farewell_server.event.dto.request.SetStockRequest;
import com.terning.farewell_server.util.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventAdminControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventAdminService eventAdminService;

    @Value("${admin.secret-key}")
    private String adminSecretKey;

    @Value("${admin.header-name}")
    private String adminHeaderName;

    @Nested
    @DisplayName("POST /api/admin/event/stock - 이벤트 재고 설정 API 테스트")
    class SetEventStockTest {

        private final String BASE_URL = "/api/admin/event/stock";

        @Test
        @DisplayName("성공: 유효한 요청과 인증 키로 재고 설정에 성공하고 200 OK를 반환한다.")
        void setEventStock_Success() throws Exception {
            // given
            int stockCount = 100;
            SetStockRequest request = new SetStockRequest(stockCount);
            String requestBody = objectMapper.writeValueAsString(request);

            when(eventAdminService.setEventStock(any(SetStockRequest.class))).thenReturn(stockCount);

            // when
            ResultActions resultActions = mockMvc.perform(post(BASE_URL)
                    .header(adminHeaderName, adminSecretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("이벤트 재고 설정에 성공했습니다."))
                    .andExpect(jsonPath("$.result.stock").value(stockCount));
        }

        @Test
        @DisplayName("실패: 인증 키가 없으면 403 Forbidden을 반환한다.")
        void setEventStock_Fail_NoAuthKey() throws Exception {
            // given
            SetStockRequest request = new SetStockRequest(100);
            String requestBody = objectMapper.writeValueAsString(request);

            // when
            ResultActions resultActions = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            resultActions
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("[EVENT ERROR] 유효하지 않은 관리자 키입니다. 접근이 거부되었습니다."));
        }

        @Test
        @DisplayName("실패: 재고 수량이 음수이면 400 Bad Request를 반환한다.")
        void setEventStock_Fail_NegativeStock() throws Exception {
            // given
            SetStockRequest request = new SetStockRequest(-1);
            String requestBody = objectMapper.writeValueAsString(request);

            // when
            ResultActions resultActions = mockMvc.perform(post(BASE_URL)
                    .header(adminHeaderName, adminSecretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("'count' 필드: 재고는 0 이상이어야 합니다."));
        }
    }
}
