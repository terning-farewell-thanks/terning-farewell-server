package com.terning.farewell_server.event.dto.response;

import com.terning.farewell_server.application.domain.Application;

public record StatusResponse(String status, String message) {

    private static final String MESSAGE_PREFIX = "신청 결과: ";

    public static StatusResponse from(Application application) {
        String statusName = application.getStatus().name();
        return new StatusResponse(statusName, MESSAGE_PREFIX + statusName);
    }
}
