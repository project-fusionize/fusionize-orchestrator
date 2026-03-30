package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.AgentConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfigTransformerTest {

    private final AgentConfigTransformer transformer = new AgentConfigTransformer();

    @Test
    void shouldConvertDescriptionToConfig() {
        // setup
        var description = new AgentConfigDescription();
        description.setName("my-agent");
        description.setDomain("agent-domain");
        description.setDescription("An AI agent");
        description.setTags(List.of("tag1", "tag2"));
        description.setModelConfigDomain("model-domain");
        description.setInstructionPrompt("You are a helpful assistant");
        description.setAllowedMcpTools(List.of("tool1", "tool2"));
        description.setRole(AgentConfig.Role.ANALYZER);
        description.setProperties(Map.of("key", "value"));

        // expectation
        AgentConfig config = transformer.toAgentConfig(description);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("my-agent");
        assertThat(config.getDomain()).isEqualTo("agent-domain");
        assertThat(config.getDescription()).isEqualTo("An AI agent");
        assertThat(config.getTags()).containsExactly("tag1", "tag2");
        assertThat(config.getModelConfigDomain()).isEqualTo("model-domain");
        assertThat(config.getInstructionPrompt()).isEqualTo("You are a helpful assistant");
        assertThat(config.getAllowedMcpTools()).containsExactly("tool1", "tool2");
        assertThat(config.getRole()).isEqualTo(AgentConfig.Role.ANALYZER);
        assertThat(config.getProperties()).containsEntry("key", "value");
    }

    @Test
    void shouldReturnNull_forNullDescription() {
        // setup
        AgentConfigDescription description = null;

        // expectation
        AgentConfig config = transformer.toAgentConfig(description);

        // validation
        assertThat(config).isNull();
    }

    @Test
    void shouldConvertConfigToDescription() {
        // setup
        var config = new AgentConfig();
        config.setName("my-agent");
        config.setDomain("agent-domain");
        config.setDescription("An AI agent");
        config.setTags(List.of("tag1", "tag2"));
        config.setModelConfigDomain("model-domain");
        config.setInstructionPrompt("You are a helpful assistant");
        config.setAllowedMcpTools(List.of("tool1", "tool2"));
        config.setRole(AgentConfig.Role.GENERATOR);
        config.setProperties(Map.of("key", "value"));

        // expectation
        AgentConfigDescription description = transformer.toAgentConfigDescription(config);

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getName()).isEqualTo("my-agent");
        assertThat(description.getDomain()).isEqualTo("agent-domain");
        assertThat(description.getDescription()).isEqualTo("An AI agent");
        assertThat(description.getTags()).containsExactly("tag1", "tag2");
        assertThat(description.getModelConfigDomain()).isEqualTo("model-domain");
        assertThat(description.getInstructionPrompt()).isEqualTo("You are a helpful assistant");
        assertThat(description.getAllowedMcpTools()).containsExactly("tool1", "tool2");
        assertThat(description.getRole()).isEqualTo(AgentConfig.Role.GENERATOR);
        assertThat(description.getProperties()).containsEntry("key", "value");
    }

    @Test
    void shouldReturnNull_forNullConfig() {
        // setup
        AgentConfig config = null;

        // expectation
        AgentConfigDescription description = transformer.toAgentConfigDescription(config);

        // validation
        assertThat(description).isNull();
    }
}
