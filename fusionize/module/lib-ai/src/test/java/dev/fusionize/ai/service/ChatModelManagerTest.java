package dev.fusionize.ai.service;

import dev.fusionize.ai.exception.*;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.repo.ChatModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatModelManagerTest {

    @Mock
    private ChatModelConfigRepository repository;


    @Mock
    private org.springframework.retry.support.RetryTemplate retryTemplate;

    @Mock
    private io.micrometer.observation.ObservationRegistry observationRegistry;

    @Mock
    private org.springframework.ai.model.tool.ToolCallingManager toolCallingManager;

    private ChatModelManager manager;

    @BeforeEach
    void setUp() {
        manager = new ChatModelManager(repository, retryTemplate, observationRegistry,
                toolCallingManager);
    }

    @Test
    void saveModel_Success() throws ChatModelException {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("openai")
                .withApiKey("sk-test")
                .withModelName("gpt-4")
                .build();
        // Domain is generated from key in builder if not set, let's ensure it is set
        config.setDomain("test.gpt-4");

        when(repository.save(any(ChatModelConfig.class))).thenReturn(config);
        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.empty());

        ChatModelConfig saved = manager.saveModel(config);
        assertNotNull(saved);
        assertEquals("gpt-4", saved.getKey());
        verify(repository).save(config);
    }

    @Test
    void saveModel_DomainExists() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("openai")
                .withApiKey("sk-test")
                .withModelName("gpt-4")
                .build();
        config.setDomain("test.gpt-4");

        when(repository.findByDomain(config.getDomain())).thenReturn(Optional.of(config));

        assertThrows(ChatModelDomainAlreadyExistsException.class, () -> manager.saveModel(config));
    }

    @Test
    void saveModel_InvalidConfig() {
        ChatModelConfig config = new ChatModelConfig(); // Empty config

        assertThrows(InvalidChatModelConfigException.class, () -> manager.saveModel(config));
    }

    @Test
    void getModel() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .build();

        when(repository.findByDomain("gpt-4")).thenReturn(Optional.of(config));

        Optional<ChatModelConfig> found = manager.getModel("gpt-4");
        assertTrue(found.isPresent());
        assertEquals("gpt-4", found.get().getKey());
    }

    @Test
    void getChatClient_OpenAi() throws ChatModelException {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("openai")
                .withApiKey("sk-test")
                .withModelName("gpt-4-turbo")
                .withTemperature(0.7)
                .build();
        config.setDomain("test.gpt-4");

        when(repository.findByDomain("test.gpt-4")).thenReturn(Optional.of(config));
        ChatClient client = manager.getChatClient("test.gpt-4");
        assertNotNull(client);
    }

    @Test
    void getChatClient_NotFound() {
        when(repository.findByDomain("unknown")).thenReturn(Optional.empty());

        assertThrows(ChatModelNotFoundException.class, () -> manager.getChatClient("unknown"));
    }

    @Test
    void getChatClient_UnsupportedProvider() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("unknown")
                .withApiKey("sk-test")
                .withModelName("gpt-4")
                .build();

        assertThrows(UnsupportedChatModelProviderException.class, () -> manager.getChatClient(config));
    }
}
