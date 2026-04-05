package dev.fusionize.ai;

import dev.fusionize.ai.service.ClassifierService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.ArrayList;
import java.util.List;

public class Classifier implements ComponentRuntime {
    public static final String CONF_AGENT_NAME = "agent";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_CATEGORIES = "categories";
    public static final String CONF_CRITERIA = "criteria";

    private final ClassifierService classifierService;

    private String inputVar = "data";
    private String outputVar = "classification";
    private String agentName;
    private String criteria;
    private List<String> categories = new ArrayList<>();

    public Classifier(ClassifierService classifierService) {
        this.classifierService = classifierService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_AGENT_NAME).ifPresent(s -> this.agentName = s);
        config.varString(CONF_CRITERIA).ifPresent(s -> this.criteria = s);
        config.varList(CONF_CATEGORIES).ifPresent(l -> {
            this.categories = new ArrayList<>();
            for (Object item : l) {
                this.categories.add(item.toString());
            }
        });
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (agentName == null || agentName.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Agent name not found in configs"));
            return;
        }
        if (categories.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Categories not found in configs"));
            return;
        }
        if (context.contains(inputVar)) {
            emitter.logger().info("Classifier is activated, classifying {} to {}", inputVar, outputVar);
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            ClassifierService.Response response = classifierService.classify(
                    new ClassifierService.ClassifyPackage(
                            context, inputVar, categories, criteria, agentName,
                            emitter.logger(), emitter.interactionLogger())
            );

            if (response == null) {
                throw new IllegalStateException("Classifier returned null response");
            }
            emitter.logger().info("Classification successful: category={}, confidence={}",
                    response.category(), response.confidence());
            context.getData().put(outputVar, response.toMap());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error classifying data: {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }
}
