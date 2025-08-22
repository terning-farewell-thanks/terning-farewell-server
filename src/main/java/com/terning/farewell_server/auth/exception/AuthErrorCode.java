package com.terning.farewell_server.auth.exception;

import com.terning.farewell_server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 인증 코드입니다.");

    private static final String PREFIX = "[AUTH ERROR] ";

    private final HttpStatus status;
    private final String rawMessage;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return PREFIX + rawMessage;
    }
}
