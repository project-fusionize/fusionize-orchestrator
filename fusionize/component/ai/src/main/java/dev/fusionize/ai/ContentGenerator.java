package dev.fusionize.ai;

import dev.fusionize.ai.service.ContentGeneratorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.HashMap;
import java.util.Map;

public class ContentGenerator implements ComponentRuntime {
    public static final String CONF_AGENT_NAME = "agent";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_TEMPLATE = "template";
    public static final String CONF_TONE = "tone";
    public static final String CONF_FORMAT = "format";

    private final ContentGeneratorService contentGeneratorService;

    private String inputVar = "data";
    private String outputVar = "generatedContent";
    private String agentName;
    private String template;
    private String tone;
    private String format;

    public ContentGenerator(ContentGeneratorService contentGeneratorService) {
        this.contentGeneratorService = contentGeneratorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_AGENT_NAME).ifPresent(s -> this.agentName = s);
        config.varString(CONF_TEMPLATE).ifPresent(s -> this.template = s);
        config.varString(CONF_TONE).ifPresent(s -> this.tone = s);
        config.varString(CONF_FORMAT).ifPresent(s -> this.format = s);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (agentName == null || agentName.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Agent name not found in configs"));
            return;
        }
        if (template == null || template.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Template not found in configs"));
            return;
        }
        if (context.contains(inputVar)) {
            emitter.logger().info("Content generator is activated, generating from {} to {}", inputVar, outputVar);
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            ContentGeneratorService.Response response = contentGeneratorService.generate(
                    new ContentGeneratorService.GeneratePackage(
                            context, inputVar, template, tone, format, agentName,
                            emitter.logger(), emitter.interactionLogger())
            );

            if (response == null) {
                throw new IllegalStateException("Content generator returned null response");
            }
            emitter.logger().info("Content generation successful, length={}", response.content().length());
            Map<String, Object> output = new HashMap<>();
            output.put("content", response.content());
            if (response.metadata() != null) {
                output.put("metadata", response.metadata());
            }
            context.getData().put(outputVar, output);
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error generating content: {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }
}
