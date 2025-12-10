package dev.fusionize.ai.exception;

import dev.fusionize.common.exception.ApplicationException;

public class ChatModelException extends ApplicationException {
    public ChatModelException(String message) {
        super(new Exception(message));
    }

    public ChatModelException(String message, Throwable cause) {
        super(new Exception(message, cause));
    }
}
