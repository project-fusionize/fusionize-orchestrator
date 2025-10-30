package dev.fusionize.workflow.events;
import dev.fusionize.common.exception.ApplicationException;

public class EventHandlerNotFoundException extends ApplicationException {
    public EventHandlerNotFoundException() {
        super();
    }

    public EventHandlerNotFoundException(String message) {
        super(message);
    }

    public EventHandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventHandlerNotFoundException(Throwable cause) {
        super(cause);
    }
}
