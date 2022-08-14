package com.services.common.exception.toresponse;

import com.services.common.constant.GlobalConstant;
import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import com.services.common.exception.BaseException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.ENTITY_CODER + 900)
public class BaseExceptionHandler {


    @ServerExceptionMapper
    public RestResponse<ErrorDetails> toResponse(Exception e) {

        e.printStackTrace();

        Span span = Span.current();

        if (e instanceof BaseException) {
            if (((BaseException) e).getErrorDetails().getLevel().ordinal() >= ErrorLevel.ERROR.ordinal()) {
                Log.error(e.getMessage(), e);
            }

            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());

            return RestResponse.status(
                    RestResponse.Status.fromStatusCode(((BaseException) e).getErrorDetails().getErrorCode().getErrorType().getStatusCode()),
                    ((BaseException) e).getErrorDetails()
            );
        }

        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        Log.error(e.getMessage(), e);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .title(e.getClass().getSimpleName())
                .level(ErrorLevel.WARNING)
                .errorCode(ErrorCode.INTERNAL_ERROR)
                .message(e.getMessage())
                .userMessage(GlobalConstant.DEFAULT_ERROR_MESSAGE)
                .build();

        return RestResponse.status(
                RestResponse.Status.fromStatusCode(errorDetails.getErrorCode().getErrorType().getStatusCode()),
                errorDetails
        );
    }

}
