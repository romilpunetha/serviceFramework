package com.services.common.exception;

import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;

public class RedisLockWaitException extends BaseRuntimeException {

    public RedisLockWaitException() {
        super(ErrorLevel.ERROR, ErrorCode.INTERNAL_ERROR, "RedisLockWaitException", "Redis lock wait failed");
    }

    public RedisLockWaitException(ErrorDetails errorDetails) {
        super(errorDetails);
    }

    public RedisLockWaitException(ErrorDetails errorDetails, Throwable throwable) {
        super(errorDetails, throwable);
    }

    public RedisLockWaitException(String message) {
        super(ErrorLevel.ERROR, ErrorCode.INTERNAL_ERROR, "RedisLockWaitException", message);
    }

    public RedisLockWaitException(String message, String userMessage) {
        super(ErrorLevel.ERROR, ErrorCode.INTERNAL_ERROR, "RedisLockWaitException", message, userMessage);
    }
}
