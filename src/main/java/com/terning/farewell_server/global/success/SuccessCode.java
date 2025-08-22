package com.terning.farewell_server.global.success;

import org.springframework.http.HttpStatus;

public interface SuccessCode {
    HttpStatus getStatus();
    String getMessage();

    default String getMessage(Object... args) {
        return String.format(this.getMessage(), args);
    }
}
