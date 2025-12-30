package dev.fusionize.workflow.agent;

import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.ai.service.ChatModelManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAgentService {
    private final ChatModelManager chatModelManager;
    private final WorkflowToolService workflowToolService;

    public WorkflowAgentService(ChatModelManager chatModelManager, WorkflowToolService workflowToolService) {
        this.chatModelManager = chatModelManager;
        this.workflowToolService = workflowToolService;
    }

    public String process(UserRequest request) throws ChatModelException {
        ChatClient client = chatModelManager.getChatClient(request.getModelConfig());
        client = client.mutate().defaultToolCallbacks(MethodToolCallbackProvider.builder()
                .toolObjects(workflowToolService)
                .build()).build();
        return client.prompt()
                .user(request.getMessage())
                .call()
                .content();

    }
}
