package dev.fusionize.workflow.component.exceptions;
import dev.fusionize.common.exception.ApplicationException;

public class ComponentNotFoundException extends ApplicationException {
    public ComponentNotFoundException() {
        super();
    }

    public ComponentNotFoundException(String message) {
        super(message);
    }

    public ComponentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentNotFoundException(Throwable cause) {
        super(cause);
    }
}
