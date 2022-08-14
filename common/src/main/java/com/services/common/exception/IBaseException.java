package com.services.common.exception;

import com.services.common.domain.exception.ErrorDetails;

public interface IBaseException {

    ErrorDetails getErrorDetails();

    void setErrorDetails(ErrorDetails errorDetails);
}
