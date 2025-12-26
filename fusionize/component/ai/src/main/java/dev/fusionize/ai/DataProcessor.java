package dev.fusionize.ai;

import dev.fusionize.ai.service.DataProcessorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.HashMap;
import java.util.Map;

public class DataProcessor implements ComponentRuntime {
    private final DataProcessorService dataProcessorService;
    public static final String CONF_AGENT_NAME = "agent";
    public static final String CONF_PROMPT = "prompt";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_EXAMPLE = "example";

    private String inputVar;
    private String outputVar;
    private String agentName;
    private String prompt;

    private Map<String, Object> example = new HashMap<>();

    public DataProcessor(DataProcessorService dataProcessorService) {
        this.dataProcessorService = dataProcessorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_PROMPT).ifPresent(s -> this.prompt = s);
        config.varString(CONF_AGENT_NAME).ifPresent(s -> this.agentName = s);
        config.varMap(CONF_EXAMPLE).ifPresent(m -> this.example = m);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if(agentName == null || agentName.isEmpty()) {
            emitter.failure(new IllegalArgumentException("Agent name not found in configs"));
            return;
        }
        if(prompt == null || prompt.isEmpty()) {
            emitter.failure(new IllegalArgumentException("prompt not found in configs"));
            return;
        }
        if (context.contains(inputVar)) {
            emitter.logger().info("Data processor is activated, processing {} to {}", inputVar, outputVar);
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            DataProcessorService.Response response = dataProcessorService.process(
                    new DataProcessorService.ProcessPackage(
                            context, inputVar, example, agentName, prompt, emitter.logger(), emitter.interactionLogger())
            );

            if (response == null) {
                throw new Exception("response is null");
            }
            emitter.logger().info("Data processing was successful, {}", response.data());
            context.getData().put(outputVar, response.data());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error processing data {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }
}
