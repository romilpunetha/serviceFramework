package com.services.common.exception;

import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseRuntimeException extends RuntimeException implements IBaseException {

    protected ErrorDetails errorDetails;

    public BaseRuntimeException(ErrorDetails errorDetails) {

        super(errorDetails.getMessage());
        this.errorDetails = errorDetails;
    }

    public BaseRuntimeException(ErrorDetails errorDetails, Throwable throwable) {

        super(errorDetails.getMessage(), throwable);
        this.errorDetails = errorDetails;
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message) {

        this(level, errorCode, title, message, null, new HashMap<>(), new ArrayList<>());
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, String userMessage) {

        this(level, errorCode, title, message, userMessage, new HashMap<>(), new ArrayList<>());
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, Throwable throwable) {

        this(level, errorCode, title, message, null, new HashMap<>(), new ArrayList<>(), throwable);
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, Map<String, Object> metadata) {

        super(message);
        this.errorDetails = ErrorDetails.builder()
                .level(level)
                .errorCode(errorCode)
                .title(title)
                .message(message)
                .metadata(metadata)
                .build();
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, String userMessage, Map<String, Object> metadata) {

        super(message);
        this.errorDetails = ErrorDetails.builder()
                .level(level)
                .errorCode(errorCode)
                .title(title)
                .message(message)
                .userMessage(userMessage)
                .metadata(metadata)
                .build();
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, String userMessage, Map<String, Object> metadata, List<Map<String, Object>> violations) {

        super(message);
        this.errorDetails = ErrorDetails.builder()
                .level(level)
                .errorCode(errorCode)
                .title(title)
                .message(message)
                .userMessage(userMessage)
                .metadata(metadata)
                .violations(violations)
                .build();
    }

    public BaseRuntimeException(ErrorLevel level, ErrorCode errorCode, String title, String message, String userMessage, Map<String, Object> metadata, List<Map<String, Object>> violations, @NotNull Throwable throwable) {

        super(message, throwable);
        this.errorDetails = ErrorDetails.builder()
                .level(level)
                .errorCode(errorCode)
                .title(title)
                .message(message)
                .userMessage(userMessage)
                .metadata(metadata)
                .violations(violations)
                .build();
        ExceptionUtil.copyExtraData(this, throwable);
    }
}
