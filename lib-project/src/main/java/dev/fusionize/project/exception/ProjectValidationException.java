package dev.fusionize.project.exception;

import dev.fusionize.common.exception.ApplicationException;

public class ProjectValidationException extends ApplicationException {
    public ProjectValidationException() {
        super();
    }

    public ProjectValidationException(String message) {
        super(message);
    }

    public ProjectValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectValidationException(Throwable cause) {
        super(cause);
    }
}
