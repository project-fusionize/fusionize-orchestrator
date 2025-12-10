package dev.fusionize.ai.exception;

public class UnsupportedChatModelProviderException extends ChatModelException {
    public UnsupportedChatModelProviderException(String provider) {
        super("Provider not supported: " + provider);
    }
}
