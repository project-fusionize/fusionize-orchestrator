package dev.fusionize.workflow.agent;

import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.ai.service.ChatModelManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkflowAgentService {
    private final ChatModelManager chatModelManager;
    private final WorkflowToolService workflowToolService;

    private static final String SYSTEM_PROMPT = """
            You are an expert Workflow Agent for the Fusionize platform.
            
            ### GOAL
            Autonomously solve the user's request by following a strict reasoning loop.

            ### INSTRUCTIONS
            1.  **Iterative Steps (max: 10 steps)**:
                - **Turn 1**: Output **THOUGHT** and **PLAN**. You MAY researched (e.g., `listAvailableWorkflowComponents`). End with "CONTINUE".
                - **Subsequent Turns**: **ACT** (tools) -> **VALIDATE** -> **REFLECT**.
            2.  **Validation**: Before finishing, you MUST compare your YAML against `getExampleWorkflowYaml` reference.
            3.  **Termination**:
                - When you have the solution, you **MUST** output: `FINAL ANSWER: [your solution]`.
                - **DO NOT** output polite closing remarks (e.g., "Let me know if you need help").
                - If you are done, JUST say "FINAL ANSWER".

            ### FORMAT
            - **THOUGHT**: ...
            - **PLAN**: ...
            - **ACT**: ...
            - **REFLECT**: ...
            - **FINAL ANSWER**: ...
            """;

    public WorkflowAgentService(ChatModelManager chatModelManager, WorkflowToolService workflowToolService) {
        this.chatModelManager = chatModelManager;
        this.workflowToolService = workflowToolService;
    }

    public String process(UserRequest request) throws ChatModelException {
        ChatClient client = chatModelManager.getChatClient(request.getModelConfig());
        client = client.mutate().defaultToolCallbacks(MethodToolCallbackProvider.builder()
                .toolObjects(workflowToolService)
                .build()).build();

        List<Message> conversation = new ArrayList<>();
        conversation.add(new SystemMessage(SYSTEM_PROMPT));
        conversation.add(new UserMessage(request.getMessage()));

        int maxTurns = 10;
        int currentTurn = 0;

        StringBuilder completeResponse = new StringBuilder();

        while (currentTurn < maxTurns) {
            currentTurn++;
            
            // Call the model with the current history
            var response = client.prompt()
                    .messages(conversation)
                    .call()
                    .chatResponse();

            AssistantMessage assistantMessage = response.getResult().getOutput();
            String content = assistantMessage.getText();
            
            conversation.add(assistantMessage);
            completeResponse.append(content).append("\n");

            // Check for termination signal
            if (content.contains("FINAL ANSWER:")) {
                // Return the part after "FINAL ANSWER:", or the whole content if ambiguous
                return content.substring(content.indexOf("FINAL ANSWER:") + 13).trim();
            }
            
            conversation.add(new UserMessage("Current step completed. If you have the final solution, output 'FINAL ANSWER:'. Otherwise, proceed to the next step of your plan."));
        }

        return "Agent reached maximum turns (" + maxTurns + ") without a final answer. Partial log:\n" + completeResponse;
    }

    public Flux<String> processStream(UserRequest request) throws ChatModelException {
        ChatClient client = chatModelManager.getChatClient(request.getModelConfig());
        client = client.mutate().defaultToolCallbacks(MethodToolCallbackProvider.builder()
                .toolObjects(workflowToolService)
                .build()).build();

        List<Message> conversation = new ArrayList<>();
        conversation.add(new SystemMessage(SYSTEM_PROMPT));
        conversation.add(new UserMessage(request.getMessage()));

        return recursiveAgentLoop(client, conversation, 0);
    }

    private Flux<String> recursiveAgentLoop(ChatClient client, List<Message> history, int currentTurn) {
        if (currentTurn >= 10) {
            return Flux.just("\n[Agent reached maximum turns limit]");
        }


        StringBuilder turnContent = new StringBuilder();

        // Stream the current turn
        return client.prompt()
                .messages(history)
                .stream()
                .content()
                .doOnNext(turnContent::append)
                .concatWith(Flux.defer(() -> {
                    String fullContent = turnContent.toString();
                    
                    // Update history with the agent's full response
                    history.add(new AssistantMessage(fullContent));

                    // Check for termination
                    if (fullContent.contains("FINAL ANSWER:")) {
                        return Flux.empty();
                    }

                    // Prompt to continue
                    history.add(new UserMessage("Current step completed. If you have the final solution, output 'FINAL ANSWER:'. Otherwise, proceed to the next step of your plan."));
                    
                    // Recursive call for next turn
                    return recursiveAgentLoop(client, history, currentTurn + 1);
                }));
    }
}
