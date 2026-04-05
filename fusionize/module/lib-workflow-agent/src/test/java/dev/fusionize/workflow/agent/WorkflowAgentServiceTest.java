package dev.fusionize.workflow.agent;

import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.ai.service.ChatModelManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.ai.tool.ToolCallbackProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowAgentServiceTest {

    @Mock
    private ChatModelManager chatModelManager;

    @Mock
    private WorkflowToolService workflowToolService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder clientBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    private WorkflowAgentService workflowAgentService;

    @BeforeEach
    void setUp() {
        workflowAgentService = new WorkflowAgentService(chatModelManager, workflowToolService);
    }

    private void setupChatClientMockChain() throws ChatModelException {
        when(chatModelManager.getChatClient(anyString())).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(clientBuilder);
        when(clientBuilder.defaultToolCallbacks(any(ToolCallbackProvider[].class))).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
    }

    private UserRequest createRequest(String message, String modelConfig) {
        var request = new UserRequest();
        request.setMessage(message);
        request.setModelConfig(modelConfig);
        return request;
    }

    @Test
    void shouldReturnFinalAnswer_whenContentContainsFinalAnswer() throws ChatModelException {
        // setup
        setupChatClientMockChain();
        var assistantMessage = new AssistantMessage("THOUGHT: analyzing request\nFINAL ANSWER: the solution");
        when(generation.getOutput()).thenReturn(assistantMessage);
        var request = createRequest("build a workflow", "gpt-4");

        // expectation
        var result = workflowAgentService.process(request);

        // validation
        assertThat(result).isEqualTo("the solution");
    }

    @Test
    void shouldReturnMaxTurnsMessage_whenNoFinalAnswer() throws ChatModelException {
        // setup
        setupChatClientMockChain();
        var assistantMessage = new AssistantMessage("THOUGHT: thinking about the problem");
        when(generation.getOutput()).thenReturn(assistantMessage);
        var request = createRequest("build a workflow", "gpt-4");

        // expectation
        var result = workflowAgentService.process(request);

        // validation
        assertThat(result).contains("maximum turns");
        assertThat(result).contains("10");
    }

    @Test
    void shouldCallChatModelManagerWithModelConfig() throws ChatModelException {
        // setup
        setupChatClientMockChain();
        var assistantMessage = new AssistantMessage("FINAL ANSWER: done");
        when(generation.getOutput()).thenReturn(assistantMessage);
        var request = createRequest("create workflow", "claude-3-opus");

        // expectation
        workflowAgentService.process(request);

        // validation
        verify(chatModelManager).getChatClient("claude-3-opus");
    }

    @Test
    void shouldReturnStreamFromProcessStream() throws ChatModelException {
        // setup
        when(chatModelManager.getChatClient(anyString())).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(clientBuilder);
        when(clientBuilder.defaultToolCallbacks(any(ToolCallbackProvider[].class))).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(chatClient);

        @SuppressWarnings("unchecked")
        ChatClient.StreamResponseSpec streamResponseSpec = org.mockito.Mockito.mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("FINAL ANSWER: streamed result"));

        var request = createRequest("build workflow", "gpt-4");

        // expectation
        var result = workflowAgentService.processStream(request);

        // validation
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Flux.class);
    }
}
