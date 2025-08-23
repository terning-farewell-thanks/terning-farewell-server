package com.terning.farewell_server.application.exception;

import com.terning.farewell_server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApplicationErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다.");

    private static final String PREFIX = "[APPLICATION ERROR] ";

    private final HttpStatus status;
    private final String rawMessage;

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }

    @Override
    public String getMessage() {
        return PREFIX + this.rawMessage;
    }
}
