package dev.fusionize.ai;

import dev.fusionize.ai.service.ContentGeneratorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentGeneratorTest {

    private ContentGenerator contentGenerator;
    private ContentGeneratorService contentGeneratorService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        contentGeneratorService = mock(ContentGeneratorService.class);
        contentGenerator = new ContentGenerator(contentGeneratorService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure tests ---

    @Test
    void configure_usesDefaults() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "Write a summary of {input}");
        contentGenerator.configure(config);

        context.set("data", "content");
        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "Generate email");
        config.set("input", "myInput");
        config.set("output", "myOutput");
        contentGenerator.configure(config);

        context.set("myInput", "content");

        ContentGeneratorService.Response response = new ContentGeneratorService.Response("Hello!", null);
        when(contentGeneratorService.generate(any())).thenReturn(response);

        contentGenerator.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("myOutput"));
    }

    // --- canActivate tests ---

    @Test
    void canActivate_failsWhenAgentNameMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("template", "some template");
        contentGenerator.configure(config);

        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenAgentNameEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "");
        config.set("template", "some template");
        contentGenerator.configure(config);

        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenTemplateMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        contentGenerator.configure(config);

        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Template"));
    }

    @Test
    void canActivate_failsWhenTemplateEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "");
        contentGenerator.configure(config);

        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "some template");
        contentGenerator.configure(config);

        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("data"));
    }

    @Test
    void canActivate_succeedsWithAllConfig() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "Write an email about {input}");
        contentGenerator.configure(config);

        context.set("data", "quarterly results");
        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWithOptionalToneAndFormat() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("template", "Write a letter");
        config.set("tone", "formal");
        config.set("format", "markdown");
        contentGenerator.configure(config);

        context.set("data", "content");
        contentGenerator.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run tests ---

    @Test
    void run_generatesContentSuccessfully() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("template", "Write a rejection email");
        contentGenerator.configure(config);

        context.set("data", Map.of("applicant", "John", "reason", "insufficient experience"));

        ContentGeneratorService.Response response = new ContentGeneratorService.Response(
                "Dear John, we regret to inform you...",
                Map.of("wordCount", 25));
        when(contentGeneratorService.generate(any())).thenReturn(response);

        contentGenerator.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("generatedContent");
        assertNotNull(result);
        assertEquals("Dear John, we regret to inform you...", result.get("content"));
        assertNotNull(result.get("metadata"));
    }

    @Test
    void run_generatesContentWithNullMetadata() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("template", "Generate a summary");
        contentGenerator.configure(config);

        context.set("data", "raw text");

        ContentGeneratorService.Response response = new ContentGeneratorService.Response(
                "Summary of the text.", null);
        when(contentGeneratorService.generate(any())).thenReturn(response);

        contentGenerator.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("generatedContent");
        assertNotNull(result);
        assertEquals("Summary of the text.", result.get("content"));
        assertFalse(result.containsKey("metadata"));
    }

    @Test
    void run_handlesNullResponse() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("template", "Generate something");
        contentGenerator.configure(config);

        context.set("data", "content");
        when(contentGeneratorService.generate(any())).thenReturn(null);

        contentGenerator.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_handlesServiceException() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("template", "Generate something");
        contentGenerator.configure(config);

        context.set("data", "content");
        when(contentGeneratorService.generate(any())).thenThrow(new RuntimeException("Model overloaded"));

        contentGenerator.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Model overloaded"));
    }
}
