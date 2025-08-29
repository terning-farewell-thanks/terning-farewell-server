package com.terning.farewell_server.event.api;

import com.terning.farewell_server.event.application.EventAdminService;
import com.terning.farewell_server.event.dto.request.SetStockRequest;
import com.terning.farewell_server.event.dto.response.SetStockResponse;
import com.terning.farewell_server.event.success.EventSuccessCode;
import com.terning.farewell_server.global.success.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/event")
public class EventAdminController {

    private final EventAdminService eventAdminService;

    @PostMapping("/stock")
    public ResponseEntity<SuccessResponse<SetStockResponse>> setEventStock(
            @Valid @RequestBody SetStockRequest request) {

        int newStock = eventAdminService.setEventStock(request);

        return ResponseEntity
                .status(EventSuccessCode.SET_EVENT_STOCK_SUCCESS.getStatus())
                .body(SuccessResponse.of(EventSuccessCode.SET_EVENT_STOCK_SUCCESS, SetStockResponse.from(newStock)));
    }
}
