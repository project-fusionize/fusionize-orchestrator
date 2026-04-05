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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataValidatorServiceTest {

    @Mock
    private AgentConfigManager agentConfigManager;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private DataValidatorService service;
    private Context context;

    @BeforeEach
    void setUp() throws ChatModelException, AgentConfigNotFoundException {
        MockitoAnnotations.openMocks(this);
        when(agentConfigManager.getChatClient(anyString())).thenReturn(chatClient);
        service = new DataValidatorService(agentConfigManager);
        context = new Context();
    }

    @Test
    void validate_returnsValidResponse() throws Exception {
        context.set("input", Map.of("name", "John", "email", "john@example.com"));

        DataValidatorService.Response mockResponse = new DataValidatorService.Response(
                true, List.of(), List.of());

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataValidatorService.Response.class))
                .thenReturn(mockResponse);

        DataValidatorService.ValidationPackage pkg = new DataValidatorService.ValidationPackage(
                context, "input", "name and email must be present", "mockAgent", null, null);
        DataValidatorService.Response response = service.validate(pkg);

        assertNotNull(response);
        assertTrue(response.valid());
        assertTrue(response.issues().isEmpty());
    }

    @Test
    void validate_returnsInvalidResponseWithIssues() throws Exception {
        context.set("input", Map.of("email", "bad"));

        DataValidatorService.Issue issue = new DataValidatorService.Issue("email", "error", "Invalid format");
        DataValidatorService.Response mockResponse = new DataValidatorService.Response(
                false, List.of(issue), List.of("Use format user@domain.com"));

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataValidatorService.Response.class))
                .thenReturn(mockResponse);

        DataValidatorService.ValidationPackage pkg = new DataValidatorService.ValidationPackage(
                context, "input", "email must be valid", "mockAgent", null, null);
        DataValidatorService.Response response = service.validate(pkg);

        assertNotNull(response);
        assertFalse(response.valid());
        assertEquals(1, response.issues().size());
        assertEquals("email", response.issues().get(0).field());
        assertEquals("error", response.issues().get(0).severity());
    }

    @Test
    void validate_throwsWhenInputMissing() {
        DataValidatorService.ValidationPackage pkg = new DataValidatorService.ValidationPackage(
                context, "missing", "rules", "mockAgent", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.validate(pkg));
    }

    @Test
    void validate_passesCorrectAgentName() throws Exception {
        context.set("input", "data");

        DataValidatorService.Response mockResponse = new DataValidatorService.Response(true, List.of(), List.of());
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataValidatorService.Response.class))
                .thenReturn(mockResponse);

        DataValidatorService.ValidationPackage pkg = new DataValidatorService.ValidationPackage(
                context, "input", "rules", "validatorAgent", null, null);
        service.validate(pkg);

        verify(agentConfigManager).getChatClient("validatorAgent");
    }

    @Test
    void responseToMap_containsAllFields() {
        DataValidatorService.Issue issue = new DataValidatorService.Issue("name", "warning", "too short");
        DataValidatorService.Response response = new DataValidatorService.Response(
                false, List.of(issue), List.of("use full name"));
        var map = response.toMap();

        assertEquals(false, map.get("valid"));
        assertNotNull(map.get("issues"));
        assertNotNull(map.get("suggestions"));
    }

    @Test
    void validate_usesComponentLoggerWhenProvided() throws Exception {
        context.set("input", "data");

        DataValidatorService.Response mockResponse = new DataValidatorService.Response(true, List.of(), List.of());
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DataValidatorService.Response.class))
                .thenReturn(mockResponse);

        var logCalled = new boolean[]{false};
        var logger = new dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter.Logger() {
            @Override
            public void log(String message, dev.fusionize.workflow.WorkflowLog.LogLevel level, Throwable throwable) {
                logCalled[0] = true;
            }
        };

        DataValidatorService.ValidationPackage pkg = new DataValidatorService.ValidationPackage(
                context, "input", "rules", "mockAgent", logger, null);
        service.validate(pkg);

        assertTrue(logCalled[0]);
    }
}
