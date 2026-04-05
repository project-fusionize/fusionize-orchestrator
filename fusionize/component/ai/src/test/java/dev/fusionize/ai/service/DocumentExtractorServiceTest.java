package dev.fusionize.ai.service;

import dev.fusionize.ai.advisors.ComponentLogAdvisor;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
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
    private AgentConfigManager agentConfigManager;

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
    void setUp() throws ChatModelException, AgentConfigNotFoundException {
        MockitoAnnotations.openMocks(this);

        when(agentConfigManager.getChatClient(anyString())).thenReturn(chatClient);
        when(configManager.getConfig(any())).thenReturn(Optional.of(new StorageConfig()));
        when(configManager.getFileStorageService(any())).thenReturn(storageService);
        service = new DocumentExtractorService(configManager, agentConfigManager);
        context = new Context();
        example = new HashMap<>();
        example.put("key", "value");
    }

    @Test
    void extract_fromContextData_bytes() throws Exception {
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        context.set("input", content);

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
    }

    @Test
    void extract_fromContextData_string() throws Exception {
        context.set("input", "test content string");

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
    }

    @Test
    void extract_fromContextData_base64String() throws Exception {
        String base64Content = Base64.getEncoder().encodeToString("test content".getBytes(StandardCharsets.UTF_8));
        context.set("input", base64Content);

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
    }

    @Test
    void extract_fromStorageResource() throws Exception {
        String storageName = "test-storage";
        String refKey = "file/path.txt";

        ContextResourceReference ref = new ContextResourceReference();
        ref.setStorage(storageName);
        ref.setReferenceKey(refKey);
        context.set("input", ref);

        byte[] fileBytes = "file content".getBytes(StandardCharsets.UTF_8);
        when(storageService.read(List.of(refKey))).thenReturn(Map.of(refKey, new ByteArrayInputStream(fileBytes)));

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
    void extract_fallbackToData_whenResourceFails() throws Exception {
        String storageName = "test-storage";
        String refKey = "file/path.txt";

        ContextResourceReference ref = new ContextResourceReference();
        ref.setStorage(storageName);
        ref.setReferenceKey(refKey);
        context.set("input", ref);
        context.set("input", "fallback content");

        when(storageService.read(List.of(refKey))).thenThrow(new RuntimeException("Storage unavailable"));

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
    }

    @Test
    void extract_throws_whenInputMissing() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                    context, "missing", example, "mockAgent", null, null);
            service.extract(pkg);
        });
    }

    @Test
    void extract_throws_whenContextNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                    null, "input", example, "mockAgent", null, null);
            service.extract(pkg);
        });
    }

    @Test
    void extract_throws_whenInputVarNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                    context, null, example, "mockAgent", null, null);
            service.extract(pkg);
        });
    }

    @Test
    void extract_throws_whenDocumentTypeUnsupported() {
        context.set("input", 12345); // Integer, not supported

        assertThrows(IllegalArgumentException.class, () -> {
            DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                    context, "input", example, "mockAgent", null, null);
            service.extract(pkg);
        });
    }

    @Test
    void extract_usesComponentLogger_whenProvided() throws Exception {
        context.set("input", "text content");

        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("k", "v"));
        when(chatClient.prompt()
                .user(any(Consumer.class))
                .advisors(any(ComponentLogAdvisor.class))
                .call()
                .entity(DocumentExtractorService.Response.class))
                .thenReturn(mockResponse);

        var logCalled = new boolean[]{false};
        var logger = new dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter.Logger() {
            @Override
            public void log(String message, dev.fusionize.workflow.WorkflowLog.LogLevel level, Throwable throwable) {
                logCalled[0] = true;
            }
        };

        DocumentExtractorService.ExtractionPackage pkg = new DocumentExtractorService.ExtractionPackage(
                context, "input", example, "mockAgent", logger, null);
        service.extract(pkg);

        assertTrue(logCalled[0]);
    }

    @Test
    void extract_handlesResourceWithEmptyStorage() throws Exception {
        ContextResourceReference ref = new ContextResourceReference();
        ref.setStorage("");
        ref.setReferenceKey("some-key");
        context.set("input", ref);
        context.set("input", "fallback text");

        DocumentExtractorService.Response mockResponse = new DocumentExtractorService.Response(Map.of("k", "v"));
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
    }
}
