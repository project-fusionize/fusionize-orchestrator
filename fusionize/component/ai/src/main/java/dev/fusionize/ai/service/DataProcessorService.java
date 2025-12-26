package dev.fusionize.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.advisors.ComponentLogAdvisor;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;

@Service
public class DataProcessorService {
    private static final Logger defaultLogger = LoggerFactory.getLogger(DataProcessorService.class);
    private final AgentConfigManager agentConfigManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataProcessorService(AgentConfigManager agentConfigManager) {
        this.agentConfigManager = agentConfigManager;
    }

    public record Response(Map<String, Object> data) implements Serializable {
    }

    public record ProcessPackage(
            Context context,
            String inputVar,
            Map<String, Object> example,
            String agent,
            String prompt,
            ComponentUpdateEmitter.Logger logger,
            ComponentUpdateEmitter.InteractionLogger interactionLogger) {
    }

    public Response process(ProcessPackage pkg) throws AgentConfigNotFoundException, ChatModelException, JsonProcessingException {
        if(!pkg.context.contains(pkg.inputVar) || pkg.context.getData().get(pkg.inputVar)==null){
            throw new IllegalArgumentException("input var not found "+pkg.inputVar);
        }
        String dataString = pkg.context.var(pkg.inputVar,Object.class).toString();
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent);
        if (pkg.logger !=  null) {
            pkg.logger.info("Processing data from: {}",dataString);
        }else {
            defaultLogger.info("Processing data from: {}",dataString);
        }
        String exampleJson = objectMapper.writeValueAsString(new DocumentExtractorService.Response(pkg.example()));

        return chatClient.prompt()
                .user(u -> u.text(
                                """
                                        Strictly follow instruction bellow and process the input.
                                        Provide the result in JSON structure: {example}

                                        Instruction:
                                        {instruction}
                                        
                                        Input:
                                        {input}
                                        """)
                        .param("instruction", pkg.prompt)
                        .param("example", exampleJson)
                        .param("input", dataString))
                .advisors(new ComponentLogAdvisor(pkg.interactionLogger))
                .call()
                .entity(Response.class);
    }
}
