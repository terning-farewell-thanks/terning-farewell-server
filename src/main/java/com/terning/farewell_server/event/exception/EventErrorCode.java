package com.terning.farewell_server.event.exception;

import com.terning.farewell_server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이메일로 등록된 신청 내역이 없습니다."),
    EVENT_CLOSED(HttpStatus.CONFLICT, "아쉽지만 선착순 마감되었습니다."),
    ALREADY_PROCESSING_REQUEST(HttpStatus.CONFLICT, "현재 다른 요청을 처리 중입니다. 잠시 후 다시 시도해주세요."),
    GIFT_STOCK_INFO_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "선물 재고 정보를 가져올 수 없습니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "락을 획득하는 동안 문제가 발생했습니다."),

    INVALID_ADMIN_KEY(HttpStatus.FORBIDDEN, "유효하지 않은 관리자 키입니다. 접근이 거부되었습니다.");

    private static final String PREFIX = "[EVENT ERROR] ";

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
