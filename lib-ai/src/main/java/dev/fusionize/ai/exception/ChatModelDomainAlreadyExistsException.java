package dev.fusionize.ai.exception;

public class ChatModelDomainAlreadyExistsException extends ChatModelException {
    public ChatModelDomainAlreadyExistsException(String domain) {
        super("Chat model config already exists for domain: " + domain);
    }
}
