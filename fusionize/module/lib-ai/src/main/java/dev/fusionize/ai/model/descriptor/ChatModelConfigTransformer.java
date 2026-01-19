package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.ChatModelConfig;

public class ChatModelConfigTransformer {

    public ChatModelConfig toChatModelConfig(ChatModelConfigDescription description) {
        if (description == null) {
            return null;
        }
        ChatModelConfig config = new ChatModelConfig();
        config.setName(description.getName());
        config.setDomain(description.getDomain());
        config.setProvider(description.getProvider());
        config.setApiKey(description.getApiKey());
        config.setProperties(description.getProperties());
        config.setCapabilities(description.getCapabilities());
        config.setModelName(description.getModelName());
        return config;
    }

    public ChatModelConfigDescription toChatModelConfigDescription(ChatModelConfig config) {
        if (config == null) {
            return null;
        }
        ChatModelConfigDescription description = new ChatModelConfigDescription();
        description.setName(config.getName());
        description.setDomain(config.getDomain());
        description.setProvider(config.getProvider());
        description.setApiKey(config.getApiKey());
        description.setProperties(config.getProperties());
        description.setCapabilities(config.getCapabilities());
        description.setModelName(config.getModelName());
        return description;
    }
}
