
package dev.fusionize.ai.service;

import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.repo.ChatModelConfigRepository;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatModelManager {

    private final ChatModelConfigRepository repository;
    private final ChatClient.Builder defaultBuilder;
    private final RetryTemplate retryTemplate;
    private final ObservationRegistry observationRegistry;
    private final ToolCallingManager toolCallingManager;

    public ChatModelManager(ChatModelConfigRepository repository,
            ChatClient.Builder defaultBuilder,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry,
            ToolCallingManager toolCallingManager) {
        this.repository = repository;
        this.defaultBuilder = defaultBuilder;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
        this.toolCallingManager = toolCallingManager;
    }

    public ChatModelConfig saveModel(ChatModelConfig config) {
        return repository.save(config);
    }

    public Optional<ChatModelConfig> getModel(String domain) {
        return repository.findByDomain(domain);
    }

    public void deleteModel(String domain) {
        repository.deleteByDomain(domain);
    }

    public List<ChatModelConfig> getAll(String domain) {
        return repository.findByDomainStartingWith(domain);
    }

    public ChatClient getChatClient(String domain) {
        ChatModelConfig config = repository.findByDomain(domain)
                .orElseThrow(() -> new IllegalArgumentException("Chat model not found for domain: " + domain));

        if ("openai".equalsIgnoreCase(config.getProvider())) {
            OpenAiApi openAiApi = OpenAiApi.builder().apiKey(config.getApiKey()).build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(config.getModelName())
                    .temperature(config.getTemperature())
                    .build();

            OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options, toolCallingManager, retryTemplate,
                    observationRegistry);
            return ChatClient.builder(chatModel).build();
        }

        throw new UnsupportedOperationException("Provider not supported: " + config.getProvider());
    }
}
