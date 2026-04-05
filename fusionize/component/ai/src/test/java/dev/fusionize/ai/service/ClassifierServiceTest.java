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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClassifierServiceTest {

    @Mock
    private AgentConfigManager agentConfigManager;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ClassifierService service;
    private Context context;

    @BeforeEach
    void setUp() throws ChatModelException, AgentConfigNotFoundException {
        MockitoAnnotations.openMocks(this);
        when(agentConfigManager.getChatClient(anyString())).thenReturn(chatClient);
        service = new ClassifierService(agentConfigManager);
        context = new Context();
    }

    @Test
    void classify_returnsResponseSuccessfully() throws Exception {
        context.set("input", "buy cheap products now!!!");

        ClassifierService.Response mockResponse = new ClassifierService.Response("spam", 0.95, "spam keywords");

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ClassifierService.Response.class))
                .thenReturn(mockResponse);

        ClassifierService.ClassifyPackage pkg = new ClassifierService.ClassifyPackage(
                context, "input", List.of("spam", "not-spam"), null, "mockAgent", null, null);
        ClassifierService.Response response = service.classify(pkg);

        assertNotNull(response);
        assertEquals("spam", response.category());
        assertEquals(0.95, response.confidence());
        assertEquals("spam keywords", response.explanation());
    }

    @Test
    void classify_withCriteria() throws Exception {
        context.set("input", "urgent server down");

        ClassifierService.Response mockResponse = new ClassifierService.Response("critical", 0.9, "server outage");

        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ClassifierService.Response.class))
                .thenReturn(mockResponse);

        ClassifierService.ClassifyPackage pkg = new ClassifierService.ClassifyPackage(
                context, "input", List.of("critical", "high", "medium", "low"),
                "Classify by business impact severity", "mockAgent", null, null);
        ClassifierService.Response response = service.classify(pkg);

        assertNotNull(response);
        assertEquals("critical", response.category());
    }

    @Test
    void classify_throwsWhenInputMissing() {
        ClassifierService.ClassifyPackage pkg = new ClassifierService.ClassifyPackage(
                context, "missing", List.of("A", "B"), null, "mockAgent", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.classify(pkg));
    }

    @Test
    void classify_passesCorrectAgentName() throws Exception {
        context.set("input", "data");

        ClassifierService.Response mockResponse = new ClassifierService.Response("A", 0.8, "reason");
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(ClassifierService.Response.class))
                .thenReturn(mockResponse);

        ClassifierService.ClassifyPackage pkg = new ClassifierService.ClassifyPackage(
                context, "input", List.of("A", "B"), null, "mySpecificAgent", null, null);
        service.classify(pkg);

        verify(agentConfigManager).getChatClient("mySpecificAgent");
    }

    @Test
    void responseToMap_containsAllFields() {
        ClassifierService.Response response = new ClassifierService.Response("A", 0.85, "because");
        var map = response.toMap();

        assertEquals("A", map.get("category"));
        assertEquals(0.85, map.get("confidence"));
        assertEquals("because", map.get("explanation"));
    }
}
