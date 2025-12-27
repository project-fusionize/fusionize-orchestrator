package dev.fusionize.ai.exception;

public class AgentConfigDomainAlreadyExistsException extends AgentConfigException {
    public AgentConfigDomainAlreadyExistsException(String domain) {
        super("Agent config already exists for domain: " + domain);
    }
}
