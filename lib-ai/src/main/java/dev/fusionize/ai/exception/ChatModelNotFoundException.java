package dev.fusionize.ai.exception;

public class ChatModelNotFoundException extends ChatModelException {
    public ChatModelNotFoundException(String domain) {
        super("Chat model not found for domain: " + domain);
    }
}
