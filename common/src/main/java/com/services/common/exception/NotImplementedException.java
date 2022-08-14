package com.services.common.exception;

import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;

public class NotImplementedException extends BaseRuntimeException {

    public NotImplementedException() {
        super(ErrorLevel.FATAL, ErrorCode.INTERNAL_ERROR, "NotImplementedException", "Operation not implemented yet");
    }

    public NotImplementedException(ErrorDetails errorDetails) {
        super(errorDetails);
    }

    public NotImplementedException(ErrorDetails errorDetails, Throwable throwable) {
        super(errorDetails, throwable);
    }

    public NotImplementedException(String message) {
        super(ErrorLevel.FATAL, ErrorCode.INTERNAL_ERROR, "NotImplementedException", message);
    }

    public NotImplementedException(String message, String userMessage) {
        super(ErrorLevel.FATAL, ErrorCode.INTERNAL_ERROR, "NotImplementedException", message, userMessage);
    }

}
