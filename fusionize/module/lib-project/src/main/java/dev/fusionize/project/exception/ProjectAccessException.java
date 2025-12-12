package dev.fusionize.project.exception;

import dev.fusionize.common.exception.ApplicationException;

public class ProjectAccessException extends ApplicationException {
    public ProjectAccessException() {
        super();
    }

    public ProjectAccessException(String message) {
        super(message);
    }

    public ProjectAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectAccessException(Throwable cause) {
        super(cause);
    }
}
