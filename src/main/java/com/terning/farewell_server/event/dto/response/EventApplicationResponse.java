package com.terning.farewell_server.event.dto.response;

public record EventApplicationResponse(String message) {
    public static EventApplicationResponse from(String message) {
        return new EventApplicationResponse(message);
    }
}
