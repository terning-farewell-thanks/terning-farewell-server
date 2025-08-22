package com.terning.farewell_server.mail.exception;

import com.terning.farewell_server.global.error.BaseException;
import com.terning.farewell_server.global.error.ErrorCode;

public class MailException extends BaseException {

    public MailException(ErrorCode errorCode) {
        super(errorCode);
    }
}
