package dev.fusionize.ai.advisors;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class ComponentLogAdvisor implements CallAdvisor, StreamAdvisor {
    private final ComponentUpdateEmitter.InteractionLogger interactionLogger;

    public ComponentLogAdvisor(ComponentUpdateEmitter.InteractionLogger interactionLogger) {
        this.interactionLogger = interactionLogger;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logMessages(chatClientRequest.prompt());

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        if(chatClientResponse.chatResponse() != null && chatClientResponse.chatResponse().getResult()!=null) {
            logMessage(chatClientResponse.chatResponse().getResult().getOutput());
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logMessages(chatClientRequest.prompt());

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, (response)->{
            if(response.chatResponse() != null && response.chatResponse().getResult()!=null) {
                logMessage(response.chatResponse().getResult().getOutput());
            }
        });
    }

    private void logMessages(Prompt prompt) {
        for (Message message : prompt.getInstructions()) {
            logMessage(message);
        }
    }
    private void logMessage(Message message) {
        WorkflowInteraction.InteractionType type = mapType(message);
        WorkflowInteraction.Visibility visibility = mapVisibility(message);
        interactionLogger.log(
                message.getText(),
                resolveActor(message),
                type, visibility);
    }

    private WorkflowInteraction.InteractionType mapType(Message message) {
        return switch (message.getMessageType()) {
            case USER, ASSISTANT -> WorkflowInteraction.InteractionType.MESSAGE;
            case SYSTEM -> WorkflowInteraction.InteractionType.THOUGHT; // policy choice
            case TOOL -> WorkflowInteraction.InteractionType.OBSERVATION;
        };
    }

    private WorkflowInteraction.Visibility mapVisibility(Message message) {
        return switch (message.getMessageType()) {
            case USER, ASSISTANT -> WorkflowInteraction.Visibility.EXTERNAL;
            case SYSTEM, TOOL -> WorkflowInteraction.Visibility.INTERNAL;
        };
    }

    private String resolveActor(Message message) {
        return switch (message.getMessageType()) {
            case USER -> "human";
            case ASSISTANT -> "ai";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

}