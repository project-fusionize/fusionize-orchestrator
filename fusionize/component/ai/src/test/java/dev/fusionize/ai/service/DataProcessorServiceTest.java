package dev.fusionize.ai.service;

import dev.fusionize.ai.advisors.ComponentLogAdvisor;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataProcessorServiceTest {

    @Mock
    private AgentConfigManager agentConfigManager;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private DataProcessorService service;
    private Context context;
    private Map<String, Object> example;

    @BeforeEach
    void setUp() throws ChatModelException, AgentConfigNotFoundException {
        MockitoAnnotations.openMocks(this);
        when(agentConfigManager.getChatClient(anyString())).thenReturn(chatClient);
        service = new DataProcessorService(agentConfigManager);
        context = new Context();
        example = new HashMap<>();
        example.put("result", "value");
    }

    @Test
    void process_returnsResponseSuccessfully() throws Exception {
        context.set("input", "some data to process");

        DataProcessorService.Response mockResponse = new DataProcessorService.Response(
                Map.of("result", "processed"));

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataProcessorService.Response.class))
                .thenReturn(mockResponse);

        DataProcessorService.ProcessPackage pkg = new DataProcessorService.ProcessPackage(
                context, "input", example, "mockAgent", "classify the data", null, null);
        DataProcessorService.Response response = service.process(pkg);

        assertNotNull(response);
        assertEquals("processed", response.data().get("result"));
    }

    @Test
    void process_throwsWhenInputMissing() {
        DataProcessorService.ProcessPackage pkg = new DataProcessorService.ProcessPackage(
                context, "missing", example, "mockAgent", "classify", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.process(pkg));
    }

    @Test
    void process_convertsInputToString() throws Exception {
        context.set("input", Map.of("nested", "value"));

        DataProcessorService.Response mockResponse = new DataProcessorService.Response(Map.of("r", "v"));
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataProcessorService.Response.class))
                .thenReturn(mockResponse);

        DataProcessorService.ProcessPackage pkg = new DataProcessorService.ProcessPackage(
                context, "input", example, "mockAgent", "classify", null, null);
        DataProcessorService.Response response = service.process(pkg);

        assertNotNull(response);
    }

    @Test
    void process_usesComponentLoggerWhenProvided() throws Exception {
        context.set("input", "data");

        DataProcessorService.Response mockResponse = new DataProcessorService.Response(Map.of("r", "v"));
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataProcessorService.Response.class))
                .thenReturn(mockResponse);

        var logCalled = new boolean[]{false};
        var logger = new dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter.Logger() {
            @Override
            public void log(String message, dev.fusionize.workflow.WorkflowLog.LogLevel level, Throwable throwable) {
                logCalled[0] = true;
            }
        };

        DataProcessorService.ProcessPackage pkg = new DataProcessorService.ProcessPackage(
                context, "input", example, "mockAgent", "classify", logger, null);
        service.process(pkg);

        assertTrue(logCalled[0]);
    }

    @Test
    void process_passesCorrectAgentName() throws Exception {
        context.set("input", "data");

        DataProcessorService.Response mockResponse = new DataProcessorService.Response(Map.of("r", "v"));
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataProcessorService.Response.class))
                .thenReturn(mockResponse);

        DataProcessorService.ProcessPackage pkg = new DataProcessorService.ProcessPackage(
                context, "input", example, "specificAgent", "classify", null, null);
        service.process(pkg);

        verify(agentConfigManager).getChatClient("specificAgent");
    }
}
