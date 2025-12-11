package dev.fusionize.ai;

import dev.fusionize.ai.service.DocumentExtractorService;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void testRun_ExtractsData() throws Exception {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        Map<String, Object> example = new HashMap<>();
        example.put("key", "value");
        config.set(DocumentExtractor.CONF_EXAMPLE, example);
        documentExtractor.configure(config);

        // Setup context
        byte[] docBytes = "test content".getBytes();
        context.set("document", docBytes);

        // Mock Service response
        DocumentExtractorService.Response response = new DocumentExtractorService.Response(Map.of(
                "key", "extractedValue"));
        when(documentExtractorService.extract(any(), any(), any(), any())).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);

        // Verify output
        Map<String, Object> result = (Map<String, Object>) context.getData().get("extractedData");
        assertNotNull(result);
        assertEquals("extractedValue", result.get("key"));
        
        verify(documentExtractorService).extract(eq(context), eq("document"), isNull(), eq(example));
    }

    @Test
    void testRun_WithStorage() throws Exception {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(DocumentExtractor.CONF_STORAGE, "my-storage");
        documentExtractor.configure(config);

        // Setup context
        context.set("document", "some ref");

        // Mock getFileStorageService
        FileStorageService mockStorage = mock(FileStorageService.class);
        when(documentExtractorService.getFileStorageService("my-storage")).thenReturn(mockStorage);
        // Re-configure to pick up the mock
        documentExtractor.configure(config);

        // Mock Service response
        DocumentExtractorService.Response response = new DocumentExtractorService.Response(Map.of("key", "val"));
        when(documentExtractorService.extract(any(), any(), any(), any())).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);
        
        verify(documentExtractorService).extract(eq(context), eq("document"), eq(mockStorage), any());
    }

    @Test
    void testRun_HandlesNullResponse() throws Exception {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        documentExtractor.configure(config);

        // Setup context
        context.set("document", "some text");

        // Mock Service response to null
        when(documentExtractorService.extract(any(), any(), any(), any())).thenReturn(null);

        // Run
        documentExtractor.run(context, emitter);

        // Verify failure
        assertTrue(emitter.failureCalled);
    }


    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        boolean failureCalled = false;

        @Override
        public void success(Context updatedContext) {
            System.err.println("SUCCESS CALLED");
            successCalled = true;
        }

        @Override
        public void failure(Exception ex) {
            System.err.println("FAILURE CALLED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            failureCalled = true;
        }

        @Override
        public ComponentUpdateEmitter.Logger logger() {
            return (message, level, throwable) -> {
                System.out.println("[" + level + "] " + message);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            };
        }
    }
}
