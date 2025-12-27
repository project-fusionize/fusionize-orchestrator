package dev.fusionize.ai.exception;

import dev.fusionize.common.exception.ApplicationException;

public class AgentConfigException extends ApplicationException {
    public AgentConfigException(String message) {
        super(new Exception(message));
    }

    public AgentConfigException(String message, Throwable cause) {
        super(new Exception(message, cause));
    }
}
