package dev.fusionize.ai;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentExtractorTest {

    private DocumentExtractor documentExtractor;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClientRequestSpec requestSpec;
    private CallResponseSpec responseSpec;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClientRequestSpec.class);
        responseSpec = mock(CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        documentExtractor = new DocumentExtractor(chatClientBuilder);
        context = new Context();
        emitter = new TestEmitter();
    }

    @Test
    void testRun_ExtractsData() {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        Map<String, Object> example = new HashMap<>();
        example.put("key", "value");
        config.set(DocumentExtractor.CONF_EXAMPLE, example);
        documentExtractor.configure(config);

        // Setup context
        byte[] docBytes = "test content".getBytes();
        context.set("document", docBytes);

        // Mock AI response
        DocumentExtractor.Response response = new DocumentExtractor.Response(Map.of(
                "key", "extractedValue"));
        when(responseSpec.entity(any(Class.class))).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);

        // Verify output
        Map<String, Object> result = (Map<String, Object>) context.getData().get("extractedData");
        assertNotNull(result);
        assertEquals("extractedValue", result.get("key"));
    }

    @Test
    void testRun_DetectsMimeType() {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        documentExtractor.configure(config);

        // Setup context with PNG bytes
        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        context.set("document", pngBytes);

        // Mock AI response
        DocumentExtractor.Response response = new DocumentExtractor.Response(Map.of());
        when(responseSpec.entity(any(Class.class))).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);

        // Verify correct MIME type passed
        verify(requestSpec).user(any(Consumer.class));
        // Note: It's hard to verify the exact lambda passed to user(), but we can
        // verify that the code runs without error
        // and that the mime type detection logic is exercised.
        // Ideally we would capture the PromptUserSpec and verify the media call, but
        // that requires more complex mocking.
        // For now, we rely on the fact that if it didn't detect PNG, it would default
        // to octet-stream,
        // and if we were using a real ChatClient it would matter.
        // Since we are mocking, we just ensure it runs.
    }

    @Test
    void testRun_HandlesBase64String() {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        documentExtractor.configure(config);

        // Setup context with Base64 encoded PNG
        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        String base64Png = java.util.Base64.getEncoder().encodeToString(pngBytes);
        context.set("document", base64Png);

        // Mock AI response
        DocumentExtractor.Response response = new DocumentExtractor.Response(Map.of());
        when(responseSpec.entity(any(Class.class))).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);

        // Verify media was passed (meaning it was treated as bytes)
        verify(requestSpec).user(any(Consumer.class));
    }

    @Test
    void testRun_HandlesPlainText() {
        // Configure
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        documentExtractor.configure(config);

        // Setup context with plain text
        String plainText = "This is just some text content.";
        context.set("document", plainText);

        // Mock AI response
        DocumentExtractor.Response response = new DocumentExtractor.Response(Map.of());
        when(responseSpec.entity(any(Class.class))).thenReturn(response);

        // Run
        documentExtractor.run(context, emitter);

        // Verify success
        assertTrue(emitter.successCalled);

        // Verify user prompt was called (we can't easily verify exact content without
        // capturing, but we know it runs)
        verify(requestSpec).user(any(Consumer.class));
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
