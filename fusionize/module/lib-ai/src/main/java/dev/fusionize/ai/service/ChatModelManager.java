package dev.fusionize.ai.service;

import dev.fusionize.ai.exception.*;
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
import org.springframework.util.StringUtils;

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

    public ChatModelConfig saveModel(ChatModelConfig config) throws ChatModelException {
        validateConfig(config);

        // Check if domain already exists for new records (id is null)
        if (config.getId() == null && repository.findByDomain(config.getDomain()).isPresent()) {
            throw new ChatModelDomainAlreadyExistsException(config.getDomain());
        }

        // For updates, ensure we are not changing the domain to one that already exists
        // on another record
        if (config.getId() != null) {
            Optional<ChatModelConfig> existing = repository.findByDomain(config.getDomain());
            if (existing.isPresent() && !existing.get().getId().equals(config.getId())) {
                throw new ChatModelDomainAlreadyExistsException(config.getDomain());
            }
        }

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

    public ChatClient getChatClient(ChatModelConfig config) throws ChatModelException {
        if ("openai".equalsIgnoreCase(config.getProvider())) {
            OpenAiApi openAiApi = OpenAiApi.builder().apiKey(config.getApiKey()).build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(config.getModelName())
                    .build();

            OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options, toolCallingManager, retryTemplate,
                    observationRegistry);
            return ChatClient.builder(chatModel).build();
        }

        throw new UnsupportedChatModelProviderException(config.getProvider());
    }

    public ChatClient getChatClient(String domain) throws ChatModelException {
        ChatModelConfig config = repository.findByDomain(domain)
                .orElseThrow(() -> new ChatModelNotFoundException(domain));

        return getChatClient(config);
    }

    public void testConnection(ChatModelConfig config) throws ChatModelException {
        try {
            ChatClient client = getChatClient(config);
            String response = client.prompt().user("Say \"Hi\"").call().content();
            if (!StringUtils.hasText(response)) {
                throw new ChatModelConnectionException("Received empty response from provider", null);
            }
        } catch (Exception e) {
            if (e instanceof ChatModelException) {
                throw (ChatModelException) e;
            }
            throw new ChatModelConnectionException("Failed to connect to chat model provider: " + e.getMessage(), e);
        }
    }

    private void validateConfig(ChatModelConfig config) throws InvalidChatModelConfigException {
        if (config == null) {
            throw new InvalidChatModelConfigException("Config cannot be null");
        }
        if (!StringUtils.hasText(config.getDomain())) {
            throw new InvalidChatModelConfigException("Domain is required");
        }
        if (!StringUtils.hasText(config.getProvider())) {
            throw new InvalidChatModelConfigException("Provider is required");
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new InvalidChatModelConfigException("API Key is required");
        }
        if (!StringUtils.hasText(config.getModelName())) {
            throw new InvalidChatModelConfigException("Model Name is required");
        }
    }
}
