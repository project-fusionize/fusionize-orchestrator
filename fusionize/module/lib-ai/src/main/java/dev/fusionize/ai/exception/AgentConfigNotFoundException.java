package dev.fusionize.ai.exception;

public class AgentConfigNotFoundException extends AgentConfigException {
    public AgentConfigNotFoundException(String domain) {
        super("Agent config not found for domain: " + domain);
    }
}
