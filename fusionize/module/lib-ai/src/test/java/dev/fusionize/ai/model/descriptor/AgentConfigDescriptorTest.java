package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.AgentConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigDescriptorTest {

    private final AgentConfigDescriptor descriptor = new AgentConfigDescriptor();

    @Test
    void shouldParseYamlToAgentConfig() {
        // setup
        String yaml = """
                name: test-agent
                domain: test-domain
                role: ANALYZER
                description: A test agent
                modelConfigDomain: model-domain
                instructionPrompt: Be helpful
                tags:
                  - tag1
                  - tag2
                allowedMcpTools:
                  - tool1
                """;

        // expectation
        AgentConfig config = descriptor.fromYamlDescription(yaml);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("test-agent");
        assertThat(config.getDomain()).isEqualTo("test-domain");
        assertThat(config.getRole()).isEqualTo(AgentConfig.Role.ANALYZER);
        assertThat(config.getDescription()).isEqualTo("A test agent");
        assertThat(config.getModelConfigDomain()).isEqualTo("model-domain");
        assertThat(config.getInstructionPrompt()).isEqualTo("Be helpful");
        assertThat(config.getTags()).containsExactly("tag1", "tag2");
        assertThat(config.getAllowedMcpTools()).containsExactly("tool1");
    }

    @Test
    void shouldSerializeAgentConfigToYaml() {
        // setup
        var config = new AgentConfig();
        config.setName("my-agent");
        config.setDomain("agent-domain");
        config.setRole(AgentConfig.Role.DECIDER);
        config.setDescription("A deciding agent");
        config.setInstructionPrompt("Make decisions");

        // expectation
        String yaml = descriptor.toYamlDescription(config);

        // validation
        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("my-agent");
        assertThat(yaml).contains("agent-domain");
        assertThat(yaml).contains("DECIDER");
        assertThat(yaml).contains("A deciding agent");
        assertThat(yaml).contains("Make decisions");
    }
}
