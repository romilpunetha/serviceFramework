package com.services.common.exception.toerror;

import com.services.common.domain.exception.ErrorDetails;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import com.services.common.exception.BaseRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.quarkus.arc.Priority;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.core.Response;

@Priority(4500)
public class FrameworkClientExceptionMapper implements ResponseExceptionMapper<Throwable> {

    @Override
    public BaseRuntimeException toThrowable(Response response) {

        Span span = Span.current();

        try {
            response.bufferEntity();
            BaseRuntimeException exception = new BaseRuntimeException(response.readEntity(ErrorDetails.class));
            span.recordException(exception);
            return exception;
        } catch (Exception e) {
            span.recordException(e);
            BaseRuntimeException exception = new BaseRuntimeException(
                    ErrorLevel.ERROR,
                    ErrorCode.get(response.getStatus()),
                    "Unhandled Client Exception",
                    response.readEntity(String.class)
            );
            span.recordException(exception);
            return exception;
        }
    }
}
