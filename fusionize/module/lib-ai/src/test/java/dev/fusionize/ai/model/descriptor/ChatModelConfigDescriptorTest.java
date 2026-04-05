package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.ChatModelConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelConfigDescriptorTest {

    private final ChatModelConfigDescriptor descriptor = new ChatModelConfigDescriptor();

    @Test
    void shouldParseYamlToChatModelConfig() {
        // setup
        String yaml = """
                name: test-model
                domain: test-domain
                provider: openai
                modelName: gpt-4
                apiKey: sk-test
                capabilities:
                  - chat
                """;

        // expectation
        ChatModelConfig config = descriptor.fromYamlDescription(yaml);

        // validation
        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("test-model");
        assertThat(config.getDomain()).isEqualTo("test-domain");
        assertThat(config.getProvider()).isEqualTo("openai");
        assertThat(config.getModelName()).isEqualTo("gpt-4");
        assertThat(config.getApiKey()).isEqualTo("sk-test");
    }

    @Test
    void shouldSerializeChatModelConfigToYaml() {
        // setup
        var config = new ChatModelConfig();
        config.setName("my-model");
        config.setDomain("model-domain");
        config.setProvider("anthropic");
        config.setModelName("claude-3");
        config.setApiKey("sk-ant-key");

        // expectation
        String yaml = descriptor.toYamlDescription(config);

        // validation
        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("my-model");
        assertThat(yaml).contains("model-domain");
        assertThat(yaml).contains("anthropic");
        assertThat(yaml).contains("claude-3");
    }
}
