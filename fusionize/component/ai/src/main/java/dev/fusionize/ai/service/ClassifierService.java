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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassifierService {
    private static final Logger log = LoggerFactory.getLogger(ClassifierService.class);
    private final AgentConfigManager agentConfigManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClassifierService(AgentConfigManager agentConfigManager) {
        this.agentConfigManager = agentConfigManager;
    }

    public record Response(String category, double confidence, String explanation) implements Serializable {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("category", category);
            map.put("confidence", confidence);
            map.put("explanation", explanation);
            return map;
        }
    }

    public record ClassifyPackage(
            Context context,
            String inputVar,
            List<String> categories,
            String criteria,
            String agent,
            ComponentUpdateEmitter.Logger logger,
            ComponentUpdateEmitter.InteractionLogger interactionLogger) {
    }

    public Response classify(ClassifyPackage pkg) throws AgentConfigNotFoundException, ChatModelException, JsonProcessingException {
        if (!pkg.context.contains(pkg.inputVar) || pkg.context.getData().get(pkg.inputVar) == null) {
            throw new IllegalArgumentException("Input var not found: " + pkg.inputVar);
        }
        String dataString = pkg.context.getData().get(pkg.inputVar).toString();
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent);

        logInfo(pkg.logger, "Classifying data into categories: {}", pkg.categories);

        String categoriesJson = objectMapper.writeValueAsString(pkg.categories);
        String criteriaInstruction = pkg.criteria != null && !pkg.criteria.isEmpty()
                ? "\n\nClassification criteria:\n" + pkg.criteria
                : "";

        return chatClient.prompt()
                .user(u -> u.text(
                                """
                                        Classify the following input into exactly one of the provided categories.
                                        Respond with a JSON object containing:
                                        - "category": the chosen category (must be one from the list)
                                        - "confidence": a number between 0.0 and 1.0 indicating confidence
                                        - "explanation": a brief explanation for the classification

                                        Categories: {categories}
                                        {criteria}

                                        Input:
                                        {input}
                                        """)
                        .param("categories", categoriesJson)
                        .param("criteria", criteriaInstruction)
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
