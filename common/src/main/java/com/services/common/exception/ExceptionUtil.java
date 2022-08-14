package com.services.common.exception;

public class ExceptionUtil {

    public static void copyExtraData(IBaseException exception, Throwable cause) {
        if (cause instanceof IBaseException && ((IBaseException) cause).getErrorDetails() != null) {
            if (
                    !((IBaseException) cause).getErrorDetails().getContext().isEmpty()
                            && exception.getErrorDetails().getContext().isEmpty()) {
                exception.getErrorDetails().setContext(((IBaseException) cause).getErrorDetails().getContext());
            }
            if (
                    !((IBaseException) cause).getErrorDetails().getFingerprint().isEmpty()
                            && exception.getErrorDetails().getFingerprint().isEmpty()) {
                exception.getErrorDetails().setFingerprint(((IBaseException) cause).getErrorDetails().getFingerprint());
            }

            ((IBaseException) cause).getErrorDetails().getViolations().forEach(t -> {
                exception.getErrorDetails().getViolations().add(t);
            });
        }
    }
}
