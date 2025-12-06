package dev.fusionize.workflow.component.exceptions;
import dev.fusionize.common.exception.ApplicationException;

public class ComponentMissmatchException extends ApplicationException {
    public ComponentMissmatchException() {
        super();
    }

    public ComponentMissmatchException(String message) {
        super(message);
    }

    public ComponentMissmatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentMissmatchException(Throwable cause) {
        super(cause);
    }
}
