package com.terning.farewell_server.event.success;

import com.terning.farewell_server.global.success.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventSuccessCode implements SuccessCode {
    GET_EVENT_STATUS_SUCCESS(HttpStatus.OK, "이벤트 신청 상태 조회를 성공했습니다."),
    EVENT_APPLICATION_ACCEPTED(HttpStatus.ACCEPTED, "신청이 정상적으로 접수되었습니다. 최종 결과는 이메일로 안내됩니다."),

    SET_EVENT_STOCK_SUCCESS(HttpStatus.OK, "이벤트 재고 설정에 성공했습니다.");


    private final HttpStatus status;
    private final String rawMessage;

    @Override
    public String getMessage() {
        return this.rawMessage;
    }
}
