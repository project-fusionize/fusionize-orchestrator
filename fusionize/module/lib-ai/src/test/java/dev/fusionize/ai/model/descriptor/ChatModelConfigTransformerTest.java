package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.ChatModelConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelConfigTransformerTest {

    private final ChatModelConfigTransformer transformer = new ChatModelConfigTransformer();

    @Test
    void shouldConvertDescriptionToConfig() {
        // setup
        var description = new ChatModelConfigDescription();
        description.setName("my-model");
        description.setDomain("model-domain");
        description.setProvider("openai");
        description.setApiKey("sk-test-key");
        description.setProperties(Map.of("temperature", 0.7));
        description.setCapabilities(Set.of("chat", "embedding"));
        description.setModelName("gpt-4");

        // expectation
        ChatModelConfig config = transformer.toChatModelConfig(description);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("my-model");
        assertThat(config.getDomain()).isEqualTo("model-domain");
        assertThat(config.getProvider()).isEqualTo("openai");
        assertThat(config.getApiKey()).isEqualTo("sk-test-key");
        assertThat(config.getProperties()).containsEntry("temperature", 0.7);
        assertThat(config.getCapabilities()).containsExactlyInAnyOrder("chat", "embedding");
        assertThat(config.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void shouldReturnNull_forNullDescription() {
        // setup
        ChatModelConfigDescription description = null;

        // expectation
        ChatModelConfig config = transformer.toChatModelConfig(description);

        // validation
        assertThat(config).isNull();
    }

    @Test
    void shouldConvertConfigToDescription() {
        // setup
        var config = new ChatModelConfig();
        config.setName("my-model");
        config.setDomain("model-domain");
        config.setProvider("anthropic");
        config.setApiKey("sk-ant-key");
        config.setProperties(Map.of("maxTokens", 1024));
        config.setCapabilities(Set.of("chat"));
        config.setModelName("claude-3");

        // expectation
        ChatModelConfigDescription description = transformer.toChatModelConfigDescription(config);

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getName()).isEqualTo("my-model");
        assertThat(description.getDomain()).isEqualTo("model-domain");
        assertThat(description.getProvider()).isEqualTo("anthropic");
        assertThat(description.getApiKey()).isEqualTo("sk-ant-key");
        assertThat(description.getProperties()).containsEntry("maxTokens", 1024);
        assertThat(description.getCapabilities()).containsExactly("chat");
        assertThat(description.getModelName()).isEqualTo("claude-3");
    }

    @Test
    void shouldReturnNull_forNullConfig() {
        // setup
        ChatModelConfig config = null;

        // expectation
        ChatModelConfigDescription description = transformer.toChatModelConfigDescription(config);

        // validation
        assertThat(description).isNull();
    }
}
