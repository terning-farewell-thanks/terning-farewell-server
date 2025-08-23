package com.terning.farewell_server.event.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventFacade {

    private final EventApplyService eventApplyService;
    public void applyForGift(String email) {
        eventApplyService.applyForGift(email);
    }
}
