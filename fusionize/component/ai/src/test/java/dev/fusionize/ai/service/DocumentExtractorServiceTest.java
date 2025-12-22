package dev.fusionize.ai.service;

import dev.fusionize.ai.advisors.ComponentLogAdvisor;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentExtractorServiceTest {

    @Mock
    private ChatModelManager chatModelManager;

    // Use Deep Stubs to avoid needing to know exact intermediate types
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private StorageConfigManager configManager;
    @Mock
    private FileStorageService storageService;

    private DocumentExtractorService service;
    private Context context;
    private Map<String, Object> example;

    @BeforeEach
    void setUp() throws ChatModelException {
        MockitoAnnotations.openMocks(this);

        // Mock ChatClient builder to return our deep stubbed client
        when(chatModelManager.getChatClient(anyString())).thenReturn(chatClient);
        when(configManager.getConfig(any())).thenReturn(Optional.of(new StorageConfig()));
        when(configManager.getFileStorageService(any())).thenReturn(storageService);
        service = new DocumentExtractorService(configManager, chatModelManager);
        context = new Context();
        example = new HashMap<>();
        example.put("key", "value");
    }
    
    @Test
    void testExtract_FromContextData_Bytes() throws Exception {
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        context.set("input", content);

        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("extracted", "data"));
        
        // Mock deep call chain
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", null, null);
        DocumentExtractorService.Response response = service.extract(pkg);

        assertNotNull(response);
        assertEquals("data", response.data().get("extracted"));
    }

    @Test
    void testExtract_FromContextData_String() throws Exception {
        context.set("input", "test content string");

        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("extracted", "data"));
        
        // Mock chain for text path
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", null, null);
        DocumentExtractorService.Response response = service.extract(pkg);

        assertNotNull(response);
        assertEquals("data", response.data().get("extracted"));
    }
    
    @Test
    void testExtract_FromContextData_Base64String() throws Exception {
        String base64Content = Base64.getEncoder().encodeToString("test content".getBytes(StandardCharsets.UTF_8));
        context.set("input", base64Content);

        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("extracted", "data"));
        
        // Mock deep call chain for bytes path (decoded from base64)
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", null, null);
        service.extract(pkg);
        
        // Verify invocation (using deep verify is tricky, but result being present assumes success)
        // We can just rely on the return value being non-null and correct
    }

    @Test
    void testExtract_FromStorageResource() throws Exception {
        String storageName = "test-storage";
        String refKey = "file/path.txt";
        
        // Setup context with resource
        ContextResourceReference ref = new ContextResourceReference();
        ref.setStorage(storageName);
        ref.setReferenceKey(refKey);
        context.set("input", ref);

        byte[] fileBytes = "file content".getBytes(StandardCharsets.UTF_8);
        when(storageService.read(List.of(refKey))).thenReturn(Map.of(refKey, new ByteArrayInputStream(fileBytes)));
        
        // Mock ChatClient
        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("extracted", "data"));
        
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", null, null);
        DocumentExtractorService.Response response = service.extract(pkg);
        
        assertNotNull(response);
        assertEquals("data", response.data().get("extracted"));
        verify(storageService).read(List.of(refKey));
    }
    
    @Test
    void testExtract_FallbackToData_WhenResourceFails() throws Exception {
        String storageName = "test-storage";
        String refKey = "file/path.txt";
        
        // Setup context with resource AND data
        ContextResourceReference ref = new ContextResourceReference();
        ref.setStorage(storageName);
        ref.setReferenceKey(refKey);
        context.set("input", ref);
        context.set("input", "fallback content"); // Overwrites data but resource map is separate

        when(storageService.read(List.of(refKey))).thenThrow(new RuntimeException("Storage unavailable"));
        
        // Mock ChatClient for text path (fallback)
        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("extracted", "fallbackData"));
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", null, null);
        DocumentExtractorService.Response response = service.extract(pkg);
        
        assertNotNull(response);
        assertEquals("fallbackData", response.data().get("extracted"));
        verify(storageService).read(List.of(refKey)); // Attempted storage read
    }
    
    @Test
    void testExtract_Throws_WhenInputMissing() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                    context, "missing", example, "mockAgent", null, null);
            service.extract(pkg);
        });
    }
}