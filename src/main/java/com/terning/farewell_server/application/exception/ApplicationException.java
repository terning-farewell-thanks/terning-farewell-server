package com.terning.farewell_server.application.exception;

import com.terning.farewell_server.global.error.BaseException;
import com.terning.farewell_server.global.error.ErrorCode;

public class ApplicationException extends BaseException {
    public ApplicationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
