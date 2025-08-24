package com.terning.farewell_server.event.api;

import com.terning.farewell_server.event.application.EventService;
import com.terning.farewell_server.event.dto.response.StatusResponse;
import com.terning.farewell_server.event.success.EventSuccessCode;
import com.terning.farewell_server.global.success.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

    private final EventService eventService;

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SuccessResponse<Void> applyForGift(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        eventService.applyForGift(email);
        return SuccessResponse.from(EventSuccessCode.EVENT_APPLICATION_ACCEPTED);
    }

    @GetMapping("/status")
    public SuccessResponse<StatusResponse> getApplicationStatus(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        StatusResponse statusResponse = eventService.getApplicationStatus(email);
        return SuccessResponse.of(EventSuccessCode.GET_EVENT_STATUS_SUCCESS, statusResponse);
    }
}
