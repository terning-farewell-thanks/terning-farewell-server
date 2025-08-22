package com.terning.farewell_server.auth.exception;

import com.terning.farewell_server.global.error.BaseException;
import com.terning.farewell_server.global.error.ErrorCode;

public class AuthException extends BaseException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
