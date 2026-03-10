package dev.fusionize.ai;

import dev.fusionize.ai.service.ClassifierService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClassifierTest {

    private Classifier classifier;
    private ClassifierService classifierService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        classifierService = mock(ClassifierService.class);
        classifier = new Classifier(classifierService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure tests ---

    @Test
    void configure_usesDefaults() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        context.set("data", "content");
        classifier.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("categories", List.of("A", "B"));
        config.set("input", "myInput");
        config.set("output", "myOutput");
        classifier.configure(config);

        context.set("myInput", "content");

        ClassifierService.Response response = new ClassifierService.Response("A", 0.95, "clearly A");
        when(classifierService.classify(any())).thenReturn(response);

        classifier.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("myOutput"));
    }

    // --- canActivate tests ---

    @Test
    void canActivate_failsWhenAgentNameMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        classifier.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenAgentNameEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "");
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        classifier.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenCategoriesMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        classifier.configure(config);

        classifier.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Categories"));
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        classifier.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("data"));
    }

    @Test
    void canActivate_succeedsWithAllConfig() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("categories", List.of("A", "B", "C"));
        classifier.configure(config);

        context.set("data", "some content");
        classifier.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWithCriteria() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("categories", List.of("urgent", "normal"));
        config.set("criteria", "classify by urgency level");
        classifier.configure(config);

        context.set("data", "help needed");
        classifier.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run tests ---

    @Test
    void run_classifiesSuccessfully() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("categories", List.of("spam", "not-spam"));
        classifier.configure(config);

        context.set("data", "buy cheap products now!!!");

        ClassifierService.Response response = new ClassifierService.Response(
                "spam", 0.98, "Contains typical spam keywords");
        when(classifierService.classify(any())).thenReturn(response);

        classifier.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("classification");
        assertNotNull(result);
        assertEquals("spam", result.get("category"));
        assertEquals(0.98, result.get("confidence"));
        assertEquals("Contains typical spam keywords", result.get("explanation"));
    }

    @Test
    void run_handlesNullResponse() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        context.set("data", "content");
        when(classifierService.classify(any())).thenReturn(null);

        classifier.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_handlesServiceException() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("categories", List.of("A", "B"));
        classifier.configure(config);

        context.set("data", "content");
        when(classifierService.classify(any())).thenThrow(new RuntimeException("AI unavailable"));

        classifier.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("AI unavailable"));
    }
}
