package dev.fusionize.web;

import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;
import dev.fusionize.workflow.context.ContextRuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class FileInboundConnectorTest {

    private FileInboundConnector connector;
    private FileInboundConnectorService fileService;
    private FileStorageService storageService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        fileService = mock(FileInboundConnectorService.class);
        storageService = mock(FileStorageService.class);
        connector = new FileInboundConnector(fileService);
        context = new Context();
        emitter = new TestEmitter();
    }

    private void setRuntimeData(String workflowId, String workflowDomain, String nodeKey) {
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowId(workflowId);
        runtimeData.setWorkflowDomain(workflowDomain);
        runtimeData.setWorkflowNodeKey(nodeKey);
        context.setRuntimeData(runtimeData);
    }

    private void configureWithStorage(String storageName, String outputKey) {
        ComponentRuntimeConfig config = mock(ComponentRuntimeConfig.class);
        when(config.varString("output")).thenReturn(Optional.ofNullable(outputKey));
        when(config.varString("storage")).thenReturn(Optional.ofNullable(storageName));
        if (storageName != null) {
            when(fileService.getFileStorageService(storageName)).thenReturn(storageService);
        }
        connector.configure(config);
    }

    // --- configure ---

    @Test
    void configure_setsStorageAndOutput() {
        configureWithStorage("my-storage", "myFile");
        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_usesDefaultOutputKey() {
        configureWithStorage("my-storage", null);
        connector.canActivate(context, emitter);
        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_handlesNullStorage() {
        ComponentRuntimeConfig config = mock(ComponentRuntimeConfig.class);
        when(config.varString("output")).thenReturn(Optional.empty());
        when(config.varString("storage")).thenReturn(Optional.empty());
        connector.configure(config);

        connector.canActivate(context, emitter);
        assertTrue(emitter.failureCalled);
    }

    // --- canActivate ---

    @Test
    void canActivate_failsWhenFileStorageServiceNull() {
        ComponentRuntimeConfig config = mock(ComponentRuntimeConfig.class);
        when(config.varString("output")).thenReturn(Optional.of("out"));
        when(config.varString("storage")).thenReturn(Optional.of("missing-storage"));
        when(fileService.getFileStorageService("missing-storage")).thenReturn(null);
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertFalse(emitter.successCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void canActivate_doesNotCallSuccessAfterFailure() {
        // This was the bug — missing return after failure
        ComponentRuntimeConfig config = mock(ComponentRuntimeConfig.class);
        when(config.varString("output")).thenReturn(Optional.empty());
        when(config.varString("storage")).thenReturn(Optional.empty());
        connector.configure(config);

        connector.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertFalse(emitter.successCalled);
    }

    @Test
    void canActivate_succeedsWithValidStorage() {
        configureWithStorage("my-storage", "fileOut");

        connector.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
        assertFalse(emitter.failureCalled);
    }

    // --- run ---

    @Test
    void run_registersListenerAndHandlesFileCallback() throws Exception {
        configureWithStorage("test-storage", "fileResource");
        setRuntimeData("wf1", null, "node1");

        connector.run(context, emitter);

        ArgumentCaptor<FileInboundConnectorService.IngestKey> keyCaptor =
                ArgumentCaptor.forClass(FileInboundConnectorService.IngestKey.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<MultipartFile>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(fileService).addListener(keyCaptor.capture(), listenerCaptor.capture());

        assertEquals("wf1", keyCaptor.getValue().workflowKey());
        assertEquals("node1", keyCaptor.getValue().workflowNodeKey());

        // Prepare storage mock
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(storageService.write(anyList())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return Map.of(keys.get(0), outputStream);
        });

        // Simulate callback
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenReturn(
                new ByteArrayInputStream("file content".getBytes(StandardCharsets.UTF_8)));
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");

        listenerCaptor.getValue().accept(multipartFile);

        // Verify storage write
        assertEquals("file content", outputStream.toString(StandardCharsets.UTF_8));

        // Verify context updated
        Optional<ContextResourceReference> refOpt = context.resource("fileResource");
        assertTrue(refOpt.isPresent());
        assertEquals("test-storage", refOpt.get().getStorage());
        assertEquals("test.txt", refOpt.get().getName());
        assertEquals("text/plain", refOpt.get().getMime());

        assertTrue(emitter.successCalled);
    }

    @Test
    void run_usesWorkflowDomainOverId() {
        configureWithStorage("storage", "out");
        setRuntimeData("wf-id", "wf-domain", "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<FileInboundConnectorService.IngestKey> keyCaptor =
                ArgumentCaptor.forClass(FileInboundConnectorService.IngestKey.class);
        verify(fileService).addListener(keyCaptor.capture(), any());

        assertEquals("wf-domain", keyCaptor.getValue().workflowKey());
    }

    @Test
    void run_failsWhenWorkflowKeyNull() {
        configureWithStorage("storage", "out");
        setRuntimeData(null, null, "node1");

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
        verify(fileService, never()).addListener(any(), any());
    }

    @Test
    void run_failsWhenNodeKeyNull() {
        configureWithStorage("storage", "out");
        setRuntimeData("wf1", null, null);

        connector.run(context, emitter);

        assertTrue(emitter.failureCalled);
        verify(fileService, never()).addListener(any(), any());
    }

    @Test
    void run_callbackFailsWhenOutputStreamNull() throws Exception {
        configureWithStorage("storage", "out");
        setRuntimeData("wf1", null, "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<MultipartFile>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(fileService).addListener(any(), listenerCaptor.capture());

        when(storageService.write(anyList())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return Map.of(keys.get(0) + "-wrong", new ByteArrayOutputStream());
        });

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");

        listenerCaptor.getValue().accept(multipartFile);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_callbackFailsOnIOException() throws Exception {
        configureWithStorage("storage", "out");
        setRuntimeData("wf1", null, "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<MultipartFile>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(fileService).addListener(any(), listenerCaptor.capture());

        when(storageService.write(anyList())).thenThrow(new IOException("Disk full"));

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");

        listenerCaptor.getValue().accept(multipartFile);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void run_usesDefaultOutputKey_whenNotConfigured() throws Exception {
        configureWithStorage("storage", null);
        setRuntimeData("wf1", null, "node1");

        connector.run(context, emitter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<MultipartFile>> listenerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(fileService).addListener(any(), listenerCaptor.capture());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(storageService.write(anyList())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return Map.of(keys.get(0), outputStream);
        });

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("doc.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        listenerCaptor.getValue().accept(multipartFile);

        // Default output key is "file"
        Optional<ContextResourceReference> refOpt = context.resource("file");
        assertTrue(refOpt.isPresent());
        assertTrue(emitter.successCalled);
    }
}
