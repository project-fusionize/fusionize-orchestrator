package dev.fusionize.project.exception;

import dev.fusionize.common.exception.ApplicationException;

public class ProjectOperationException extends ApplicationException {
    public ProjectOperationException() {
        super();
    }

    public ProjectOperationException(String message) {
        super(message);
    }

    public ProjectOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectOperationException(Throwable cause) {
        super(cause);
    }
}
