package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.common.parser.descriptor.YamlDescriptor;

public class ChatModelConfigDescriptor implements YamlDescriptor<ChatModelConfig> {

    private final YamlParser<ChatModelConfigDescription> yamlParser = new YamlParser<>();

    @Override
    public ChatModelConfig fromYamlDescription(String yaml) {
        ChatModelConfigDescription description = yamlParser.fromYaml(yaml, ChatModelConfigDescription.class);
        return new ChatModelConfigTransformer().toChatModelConfig(description);
    }

    @Override
    public String toYamlDescription(ChatModelConfig model) {
        ChatModelConfigDescription description = new ChatModelConfigTransformer().toChatModelConfigDescription(model);
        return yamlParser.toYaml(description);
    }
}
