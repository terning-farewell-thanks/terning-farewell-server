package com.terning.farewell_server.event.success;

import com.terning.farewell_server.global.success.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventSuccessCode implements SuccessCode {
    EVENT_APPLICATION_ACCEPTED(HttpStatus.ACCEPTED, "신청이 정상적으로 접수되었습니다. 최종 결과는 이메일로 안내됩니다.");

    private final HttpStatus status;
    private final String rawMessage;

    @Override
    public String getMessage() {
        return this.rawMessage;
    }
}
