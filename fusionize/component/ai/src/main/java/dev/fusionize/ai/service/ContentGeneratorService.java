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
import java.util.Map;

@Service
public class ContentGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(ContentGeneratorService.class);
    private final AgentConfigManager agentConfigManager;

    public ContentGeneratorService(AgentConfigManager agentConfigManager) {
        this.agentConfigManager = agentConfigManager;
    }

    public record Response(String content, Map<String, Object> metadata) implements Serializable {
    }

    public record GeneratePackage(
            Context context,
            String inputVar,
            String template,
            String tone,
            String format,
            String agent,
            ComponentUpdateEmitter.Logger logger,
            ComponentUpdateEmitter.InteractionLogger interactionLogger) {
    }

    public Response generate(GeneratePackage pkg) throws AgentConfigNotFoundException, ChatModelException {
        if (!pkg.context.contains(pkg.inputVar) || pkg.context.getData().get(pkg.inputVar) == null) {
            throw new IllegalArgumentException("Input var not found: " + pkg.inputVar);
        }
        String dataString = pkg.context.getData().get(pkg.inputVar).toString();
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent);

        logInfo(pkg.logger, "Generating content using template");

        String toneInstruction = pkg.tone != null && !pkg.tone.isEmpty()
                ? "\nTone: " + pkg.tone
                : "";
        String formatInstruction = pkg.format != null && !pkg.format.isEmpty()
                ? "\nOutput format: " + pkg.format
                : "";

        return chatClient.prompt()
                .user(u -> u.text(
                                """
                                        Generate content based on the following template and input data.
                                        Respond with a JSON object containing:
                                        - "content": the generated text content
                                        - "metadata": optional metadata about the generation (e.g. word count, language)

                                        Template/Instructions:
                                        {template}
                                        {tone}
                                        {format}

                                        Input data:
                                        {input}
                                        """)
                        .param("template", pkg.template)
                        .param("tone", toneInstruction)
                        .param("format", formatInstruction)
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
