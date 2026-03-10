package dev.fusionize.ai;

import dev.fusionize.ai.service.DataProcessorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataProcessorTest {

    private DataProcessor dataProcessor;
    private DataProcessorService dataProcessorService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        dataProcessorService = mock(DataProcessorService.class);
        dataProcessor = new DataProcessor(dataProcessorService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure tests ---

    @Test
    void configure_usesDefaults() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "do something");
        dataProcessor.configure(config);

        context.set("data", "content");
        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "process it");
        config.set("input", "myInput");
        config.set("output", "myOutput");
        dataProcessor.configure(config);

        context.set("myInput", "content");

        DataProcessorService.Response response = new DataProcessorService.Response(Map.of("k", "v"));
        when(dataProcessorService.process(any())).thenReturn(response);

        dataProcessor.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("myOutput"));
    }

    @Test
    void configure_setsExample() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "process it");
        Map<String, Object> example = new HashMap<>();
        example.put("field", "value");
        config.set("example", example);
        dataProcessor.configure(config);

        context.set("data", "content");
        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- canActivate tests ---

    @Test
    void canActivate_failsWhenAgentNameMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("prompt", "do something");
        dataProcessor.configure(config);

        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalArgumentException.class, emitter.lastFailure);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenAgentNameEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "");
        config.set("prompt", "do something");
        dataProcessor.configure(config);

        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenPromptMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        dataProcessor.configure(config);

        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Prompt"));
    }

    @Test
    void canActivate_failsWhenPromptEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "");
        dataProcessor.configure(config);

        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Prompt"));
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "do something");
        dataProcessor.configure(config);

        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("data"));
    }

    @Test
    void canActivate_succeedsWithAllConfigAndInput() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "do something");
        dataProcessor.configure(config);

        context.set("data", "some content");
        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWithCustomInputVar() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("prompt", "do something");
        config.set("input", "customInput");
        dataProcessor.configure(config);

        context.set("customInput", "some content");
        dataProcessor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run tests ---

    @Test
    void run_processesDataSuccessfully() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("prompt", "classify this");
        Map<String, Object> example = new HashMap<>();
        example.put("category", "A");
        config.set("example", example);
        dataProcessor.configure(config);

        context.set("data", "raw data to process");

        DataProcessorService.Response response = new DataProcessorService.Response(
                Map.of("category", "B"));
        when(dataProcessorService.process(any())).thenReturn(response);

        dataProcessor.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("processedData");
        assertNotNull(result);
        assertEquals("B", result.get("category"));
        verify(dataProcessorService).process(any());
    }

    @Test
    void run_handlesNullResponse() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("prompt", "process");
        dataProcessor.configure(config);

        context.set("data", "content");
        when(dataProcessorService.process(any())).thenReturn(null);

        dataProcessor.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_handlesServiceException() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("prompt", "process");
        dataProcessor.configure(config);

        context.set("data", "content");
        when(dataProcessorService.process(any())).thenThrow(new RuntimeException("Service error"));

        dataProcessor.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Service error"));
    }
}
