package dev.fusionize.ai;

import dev.fusionize.ai.service.DocumentExtractorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentExtractorTest {

    private DocumentExtractor documentExtractor;
    private DocumentExtractorService documentExtractorService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        documentExtractorService = mock(DocumentExtractorService.class);
        documentExtractor = new DocumentExtractor(documentExtractorService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure tests ---

    @Test
    void configure_usesDefaults_whenNoConfigProvided() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        documentExtractor.configure(config);

        context.set("document", "content");
        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("input", "myInput");
        config.set("output", "myOutput");
        documentExtractor.configure(config);

        context.set("myInput", "content");

        DocumentExtractorService.Response response = new DocumentExtractorService.Response(Map.of("k", "v"));
        when(documentExtractorService.extract(any())).thenReturn(response);

        documentExtractor.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("myOutput"));
    }

    // --- canActivate tests ---

    @Test
    void canActivate_failsWhenAgentNameMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        documentExtractor.configure(config);

        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalArgumentException.class, emitter.lastFailure);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenAgentNameEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "");
        documentExtractor.configure(config);

        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        documentExtractor.configure(config);

        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("document"));
    }

    @Test
    void canActivate_succeedsWhenInputInContextData() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        documentExtractor.configure(config);

        context.set("document", "some content");
        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWhenInputInContextResources() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        documentExtractor.configure(config);

        context.getResources().put("document", new dev.fusionize.workflow.context.ContextResourceReference());
        documentExtractor.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run tests ---

    @Test
    void run_extractsDataSuccessfully() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        Map<String, Object> example = new HashMap<>();
        example.put("key", "value");
        config.set(DocumentExtractor.CONF_EXAMPLE, example);
        documentExtractor.configure(config);

        context.set("document", "test content".getBytes());

        DocumentExtractorService.Response response = new DocumentExtractorService.Response(
                Map.of("key", "extractedValue"));
        when(documentExtractorService.extract(any())).thenReturn(response);

        documentExtractor.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("extractedData");
        assertNotNull(result);
        assertEquals("extractedValue", result.get("key"));
        verify(documentExtractorService).extract(any());
    }

    @Test
    void run_handlesNullResponse() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        documentExtractor.configure(config);

        context.set("document", "some text");

        when(documentExtractorService.extract(any())).thenReturn(null);

        documentExtractor.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_handlesServiceException() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        documentExtractor.configure(config);

        context.set("document", "some text");

        when(documentExtractorService.extract(any())).thenThrow(new RuntimeException("AI service down"));

        documentExtractor.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("AI service down"));
    }
}
