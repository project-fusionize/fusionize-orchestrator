package dev.fusionize.ai.advisors;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentLogAdvisorTest {

    private ComponentLogAdvisor advisor;
    private List<LogEntry> logEntries;

    record LogEntry(Object content, String actor,
                    WorkflowInteraction.InteractionType type,
                    WorkflowInteraction.Visibility visibility) {
    }

    @BeforeEach
    void setUp() {
        logEntries = new ArrayList<>();
        ComponentUpdateEmitter.InteractionLogger interactionLogger =
                (content, actor, type, visibility) ->
                        logEntries.add(new LogEntry(content, actor, type, visibility));
        advisor = new ComponentLogAdvisor(interactionLogger);
    }

    @Test
    void getName_returnsClassName() {
        assertEquals("ComponentLogAdvisor", advisor.getName());
    }

    @Test
    void getOrder_returnsZero() {
        assertEquals(0, advisor.getOrder());
    }

    @Test
    void adviseCall_logsUserMessage() {
        UserMessage userMsg = new UserMessage("Hello");
        Prompt prompt = new Prompt(List.of(userMsg));

        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);
        when(response.chatResponse()).thenReturn(null);
        when(chain.nextCall(request)).thenReturn(response);

        advisor.adviseCall(request, chain);

        assertEquals(1, logEntries.size());
        assertEquals("human", logEntries.get(0).actor());
        assertEquals(WorkflowInteraction.InteractionType.MESSAGE, logEntries.get(0).type());
        assertEquals(WorkflowInteraction.Visibility.EXTERNAL, logEntries.get(0).visibility());
    }

    @Test
    void adviseCall_logsSystemMessage() {
        SystemMessage sysMsg = new SystemMessage("You are a helpful assistant");
        Prompt prompt = new Prompt(List.of(sysMsg));

        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);
        when(response.chatResponse()).thenReturn(null);
        when(chain.nextCall(request)).thenReturn(response);

        advisor.adviseCall(request, chain);

        assertEquals(1, logEntries.size());
        assertEquals("system", logEntries.get(0).actor());
        assertEquals(WorkflowInteraction.InteractionType.THOUGHT, logEntries.get(0).type());
        assertEquals(WorkflowInteraction.Visibility.INTERNAL, logEntries.get(0).visibility());
    }

    @Test
    void adviseCall_logsResponseMessage() {
        UserMessage userMsg = new UserMessage("Hello");
        Prompt prompt = new Prompt(List.of(userMsg));

        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        AssistantMessage assistantMsg = new AssistantMessage("Hi there");
        Generation generation = new Generation(assistantMsg);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse clientResponse = mock(ChatClientResponse.class);
        when(clientResponse.chatResponse()).thenReturn(chatResponse);
        when(chain.nextCall(request)).thenReturn(clientResponse);

        advisor.adviseCall(request, chain);

        // 1 user message + 1 assistant response
        assertEquals(2, logEntries.size());
        assertEquals("ai", logEntries.get(1).actor());
        assertEquals(WorkflowInteraction.InteractionType.MESSAGE, logEntries.get(1).type());
        assertEquals(WorkflowInteraction.Visibility.EXTERNAL, logEntries.get(1).visibility());
    }

    @Test
    void adviseCall_logsMultiplePromptMessages() {
        SystemMessage sysMsg = new SystemMessage("system prompt");
        UserMessage userMsg = new UserMessage("user input");
        Prompt prompt = new Prompt(List.of(sysMsg, userMsg));

        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);
        when(response.chatResponse()).thenReturn(null);
        when(chain.nextCall(request)).thenReturn(response);

        advisor.adviseCall(request, chain);

        assertEquals(2, logEntries.size());
        assertEquals("system", logEntries.get(0).actor());
        assertEquals("human", logEntries.get(1).actor());
    }

    @Test
    void adviseCall_chainsToNextAdvisor() {
        Prompt prompt = new Prompt(List.of(new UserMessage("test")));
        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse expectedResponse = mock(ChatClientResponse.class);
        when(expectedResponse.chatResponse()).thenReturn(null);
        when(chain.nextCall(request)).thenReturn(expectedResponse);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertSame(expectedResponse, result);
        verify(chain).nextCall(request);
    }

    @Test
    void adviseCall_handlesNullChatResponse() {
        Prompt prompt = new Prompt(List.of(new UserMessage("test")));
        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);
        when(response.chatResponse()).thenReturn(null);
        when(chain.nextCall(request)).thenReturn(response);

        assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
        // Only prompt message logged, no response
        assertEquals(1, logEntries.size());
    }

    @Test
    void adviseCall_handlesNullResult() {
        Prompt prompt = new Prompt(List.of(new UserMessage("test")));
        ChatClientRequest request = mock(ChatClientRequest.class);
        when(request.prompt()).thenReturn(prompt);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(null);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse clientResponse = mock(ChatClientResponse.class);
        when(clientResponse.chatResponse()).thenReturn(chatResponse);
        when(chain.nextCall(request)).thenReturn(clientResponse);

        assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
        assertEquals(1, logEntries.size());
    }
}
