package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.AgentConfig;

public class AgentConfigTransformer {

    public AgentConfig toAgentConfig(AgentConfigDescription description) {
        if (description == null) {
            return null;
        }
        AgentConfig config = new AgentConfig();
        config.setName(description.getName());
        config.setDomain(description.getDomain());
        config.setDescription(description.getDescription());
        config.setTags(description.getTags());
        config.setModelConfigDomain(description.getModelConfigDomain());
        config.setInstructionPrompt(description.getInstructionPrompt());
        config.setAllowedMcpTools(description.getAllowedMcpTools());
        config.setRole(description.getRole());
        config.setProperties(description.getProperties());
        return config;
    }

    public AgentConfigDescription toAgentConfigDescription(AgentConfig config) {
        if (config == null) {
            return null;
        }
        AgentConfigDescription description = new AgentConfigDescription();
        description.setName(config.getName());
        description.setDomain(config.getDomain());
        description.setDescription(config.getDescription());
        description.setTags(config.getTags());
        description.setModelConfigDomain(config.getModelConfigDomain());
        description.setInstructionPrompt(config.getInstructionPrompt());
        description.setAllowedMcpTools(config.getAllowedMcpTools());
        description.setRole(config.getRole());
        description.setProperties(config.getProperties());
        // Note: kind and apiVersion are typically set when processing/serializing,
        // or defaulted if needed.
        return description;
    }
}
