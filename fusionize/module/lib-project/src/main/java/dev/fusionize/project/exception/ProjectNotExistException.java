package dev.fusionize.project.exception;

import dev.fusionize.common.exception.ApplicationException;

public class ProjectNotExistException extends ApplicationException {
    public ProjectNotExistException() {
        super();
    }

    public ProjectNotExistException(String message) {
        super(message);
    }

    public ProjectNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectNotExistException(Throwable cause) {
        super(cause);
    }
}
