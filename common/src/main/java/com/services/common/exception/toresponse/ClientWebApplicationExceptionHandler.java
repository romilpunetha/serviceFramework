package com.services.common.exception.toresponse;

import com.services.common.constant.GlobalConstant;
import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.ENTITY_CODER + 800)
public class ClientWebApplicationExceptionHandler {


    @ServerExceptionMapper
    public RestResponse<ErrorDetails> toResponse(ClientWebApplicationException e) {

        e.printStackTrace();
        Span span = Span.current();
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        Log.error(e.getMessage(), e);
        ErrorDetails errorDetails = ErrorDetails.builder()
                .title(e.getClass().getSimpleName())
                .errorCode(ErrorCode.get(e.getResponse().getStatus()))
                .level(ErrorLevel.ERROR)
                .message(e.getMessage())
                .userMessage(GlobalConstant.DEFAULT_ERROR_MESSAGE)
                .build();

        return RestResponse.status(
                RestResponse.Status.fromStatusCode(errorDetails.getErrorCode().getErrorType().getStatusCode()),
                errorDetails);
    }
}
