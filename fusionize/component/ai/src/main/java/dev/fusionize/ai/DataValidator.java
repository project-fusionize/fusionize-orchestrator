package dev.fusionize.ai;

import dev.fusionize.ai.service.DataValidatorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

public class DataValidator implements ComponentRuntime {
    public static final String CONF_AGENT_NAME = "agent";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_RULES = "rules";

    private final DataValidatorService dataValidatorService;

    private String inputVar = "data";
    private String outputVar = "validation";
    private String agentName;
    private String rules;

    public DataValidator(DataValidatorService dataValidatorService) {
        this.dataValidatorService = dataValidatorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_AGENT_NAME).ifPresent(s -> this.agentName = s);
        config.varString(CONF_RULES).ifPresent(s -> this.rules = s);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (agentName == null || agentName.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Agent name not found in configs"));
            return;
        }
        if (rules == null || rules.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Validation rules not found in configs"));
            return;
        }
        if (context.contains(inputVar)) {
            emitter.logger().info("Data validator is activated, validating {} to {}", inputVar, outputVar);
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            DataValidatorService.Response response = dataValidatorService.validate(
                    new DataValidatorService.ValidationPackage(
                            context, inputVar, rules, agentName,
                            emitter.logger(), emitter.interactionLogger())
            );

            if (response == null) {
                throw new IllegalStateException("Data validator returned null response");
            }
            emitter.logger().info("Validation complete: valid={}, issues={}",
                    response.valid(), response.issues() != null ? response.issues().size() : 0);
            context.getData().put(outputVar, response.toMap());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error validating data: {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }
}
