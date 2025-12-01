package dev.fusionize.ai.service;

import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.repo.ChatModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatModelManagerTest {

    @Mock
    private ChatModelConfigRepository repository;

    @Mock
    private ChatClient.Builder defaultBuilder;

    @Mock
    private org.springframework.retry.support.RetryTemplate retryTemplate;

    @Mock
    private io.micrometer.observation.ObservationRegistry observationRegistry;

    @Mock
    private org.springframework.ai.model.tool.ToolCallingManager toolCallingManager;

    private ChatModelManager manager;

    @BeforeEach
    void setUp() {
        manager = new ChatModelManager(repository, defaultBuilder, retryTemplate, observationRegistry,
                toolCallingManager);
    }

    @Test
    void saveModel() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("openai")
                .withApiKey("sk-test")
                .build();

        when(repository.save(any(ChatModelConfig.class))).thenReturn(config);

        ChatModelConfig saved = manager.saveModel(config);
        assertNotNull(saved);
        assertEquals("gpt-4", saved.getKey());
        verify(repository).save(config);
    }

    @Test
    void getModel() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .build();

        when(repository.findByKey("gpt-4")).thenReturn(Optional.of(config));

        Optional<ChatModelConfig> found = manager.getModel("gpt-4");
        assertTrue(found.isPresent());
        assertEquals("gpt-4", found.get().getKey());
    }

    @Test
    void getChatClient_OpenAi() {
        ChatModelConfig config = ChatModelConfig.builder("test")
                .withKey("gpt-4")
                .withProvider("openai")
                .withApiKey("sk-test")
                .withModelName("gpt-4-turbo")
                .withTemperature(0.7)
                .build();

        when(repository.findByKey("gpt-4")).thenReturn(Optional.of(config));
        ChatClient client = manager.getChatClient("gpt-4");
        assertNotNull(client);
    }

    @Test
    void getChatClient_NotFound() {
        when(repository.findByKey("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> manager.getChatClient("unknown"));
    }
}
