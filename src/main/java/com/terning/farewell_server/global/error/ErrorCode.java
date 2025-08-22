package com.terning.farewell_server.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getStatus();
    String getMessage();

    default String getMessage(Object... args) {
        return String.format(this.getMessage(), args);
    }
}
