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

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentGeneratorServiceTest {

    @Mock
    private AgentConfigManager agentConfigManager;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ContentGeneratorService service;
    private Context context;

    @BeforeEach
    void setUp() throws ChatModelException, AgentConfigNotFoundException {
        MockitoAnnotations.openMocks(this);
        when(agentConfigManager.getChatClient(anyString())).thenReturn(chatClient);
        service = new ContentGeneratorService(agentConfigManager);
        context = new Context();
    }

    @Test
    void generate_returnsContentSuccessfully() throws Exception {
        context.set("input", Map.of("applicant", "John", "position", "Engineer"));

        ContentGeneratorService.Response mockResponse = new ContentGeneratorService.Response(
                "Dear John, Thank you for applying...", Map.of("wordCount", 15));

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ContentGeneratorService.Response.class))
                .thenReturn(mockResponse);

        ContentGeneratorService.GeneratePackage pkg = new ContentGeneratorService.GeneratePackage(
                context, "input", "Write a rejection email for {applicant} applying for {position}",
                null, null, "mockAgent", null, null);
        ContentGeneratorService.Response response = service.generate(pkg);

        assertNotNull(response);
        assertTrue(response.content().contains("John"));
        assertNotNull(response.metadata());
    }

    @Test
    void generate_withToneAndFormat() throws Exception {
        context.set("input", "quarterly results");

        ContentGeneratorService.Response mockResponse = new ContentGeneratorService.Response(
                "# Q4 Report\n\nRevenue increased...", null);

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ContentGeneratorService.Response.class))
                .thenReturn(mockResponse);

        ContentGeneratorService.GeneratePackage pkg = new ContentGeneratorService.GeneratePackage(
                context, "input", "Write a report", "formal", "markdown",
                "mockAgent", null, null);
        ContentGeneratorService.Response response = service.generate(pkg);

        assertNotNull(response);
        assertNotNull(response.content());
    }

    @Test
    void generate_throwsWhenInputMissing() {
        ContentGeneratorService.GeneratePackage pkg = new ContentGeneratorService.GeneratePackage(
                context, "missing", "template", null, null, "mockAgent", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.generate(pkg));
    }

    @Test
    void generate_passesCorrectAgentName() throws Exception {
        context.set("input", "data");

        ContentGeneratorService.Response mockResponse = new ContentGeneratorService.Response("content", null);
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ContentGeneratorService.Response.class))
                .thenReturn(mockResponse);

        ContentGeneratorService.GeneratePackage pkg = new ContentGeneratorService.GeneratePackage(
                context, "input", "template", null, null, "writerAgent", null, null);
        service.generate(pkg);

        verify(agentConfigManager).getChatClient("writerAgent");
    }

    @Test
    void generate_usesComponentLoggerWhenProvided() throws Exception {
        context.set("input", "data");

        ContentGeneratorService.Response mockResponse = new ContentGeneratorService.Response("result", null);
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ContentGeneratorService.Response.class))
                .thenReturn(mockResponse);

        var logCalled = new boolean[]{false};
        var logger = new dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter.Logger() {
            @Override
            public void log(String message, dev.fusionize.workflow.WorkflowLog.LogLevel level, Throwable throwable) {
                logCalled[0] = true;
            }
        };

        ContentGeneratorService.GeneratePackage pkg = new ContentGeneratorService.GeneratePackage(
                context, "input", "template", null, null, "mockAgent", logger, null);
        service.generate(pkg);

        assertTrue(logCalled[0]);
    }
}
