package dev.fusionize.ai.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataValidatorService {
    private static final Logger log = LoggerFactory.getLogger(DataValidatorService.class);
    private final AgentConfigManager agentConfigManager;

    public DataValidatorService(AgentConfigManager agentConfigManager) {
        this.agentConfigManager = agentConfigManager;
    }

    public record Response(boolean valid, List<Issue> issues, List<String> suggestions) implements Serializable {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("valid", valid);
            map.put("issues", issues);
            map.put("suggestions", suggestions);
            return map;
        }
    }

    public record Issue(String field, String severity, String message) implements Serializable {
    }

    public record ValidationPackage(
            Context context,
            String inputVar,
            String rules,
            String agent,
            ComponentUpdateEmitter.Logger logger,
            ComponentUpdateEmitter.InteractionLogger interactionLogger) {
    }

    public Response validate(ValidationPackage pkg) throws AgentConfigNotFoundException, ChatModelException {
        if (!pkg.context.contains(pkg.inputVar) || pkg.context.getData().get(pkg.inputVar) == null) {
            throw new IllegalArgumentException("Input var not found: " + pkg.inputVar);
        }
        String dataString = pkg.context.getData().get(pkg.inputVar).toString();
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent);

        logInfo(pkg.logger, "Validating data against rules");

        return chatClient.prompt()
                .user(u -> u.text(
                                """
                                        Validate the following input data against the provided rules.
                                        Respond with a JSON object containing:
                                        - "valid": true if all rules pass, false otherwise
                                        - "issues": array of objects with "field", "severity" (error/warning/info), and "message"
                                        - "suggestions": array of strings with improvement suggestions

                                        Validation rules:
                                        {rules}

                                        Input data:
                                        {input}
                                        """)
                        .param("rules", pkg.rules)
                        .param("input", dataString))
                .advisors(new ComponentLogAdvisor(pkg.interactionLogger))
                .call()
                .entity(Response.class);
    }

    private void logInfo(ComponentUpdateEmitter.Logger logger, String message, Object... args) {
        if (logger != null) {
            logger.info(message, args);
        } else {
            log.info(message, args);
        }
    }
}
