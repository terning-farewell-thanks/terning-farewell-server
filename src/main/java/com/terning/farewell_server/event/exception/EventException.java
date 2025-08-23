package com.terning.farewell_server.event.exception;

import com.terning.farewell_server.global.error.BaseException;
import com.terning.farewell_server.global.error.ErrorCode;

public class EventException extends BaseException {
    public EventException(ErrorCode errorCode) {
        super(errorCode);
    }
}
