package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.common.parser.descriptor.YamlDescriptor;

public class AgentConfigDescriptor implements YamlDescriptor<AgentConfig> {

    private final YamlParser<AgentConfigDescription> yamlParser = new YamlParser<>();

    @Override
    public AgentConfig fromYamlDescription(String yaml) {
        AgentConfigDescription description = yamlParser.fromYaml(yaml, AgentConfigDescription.class);
        return new AgentConfigTransformer().toAgentConfig(description);
    }

    @Override
    public String toYamlDescription(AgentConfig model) {
        AgentConfigDescription description = new AgentConfigTransformer().toAgentConfigDescription(model);
        return yamlParser.toYaml(description);
    }
}
