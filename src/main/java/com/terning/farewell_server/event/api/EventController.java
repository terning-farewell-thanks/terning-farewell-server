package com.terning.farewell_server.event.api;

import com.terning.farewell_server.auth.application.AuthService;
import com.terning.farewell_server.auth.jwt.JwtUtil;
import com.terning.farewell_server.event.application.EventFacade;
import com.terning.farewell_server.event.success.EventSuccessCode;
import com.terning.farewell_server.global.success.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

    private final EventFacade eventFacade;
    private final AuthService authService;

    @PostMapping("/apply")
    public SuccessResponse<Void> applyForGift(
            @RequestHeader(JwtUtil.AUTHORIZATION_HEADER) String authorizationHeader) {

        String email = authService.getEmailFromToken(authorizationHeader);
        eventFacade.applyForGift(email);

        return SuccessResponse.from(EventSuccessCode.EVENT_APPLICATION_ACCEPTED);
    }
}
