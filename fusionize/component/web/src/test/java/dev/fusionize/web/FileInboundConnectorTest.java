package dev.fusionize.web;

import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;
import dev.fusionize.workflow.context.ContextRuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class FileInboundConnectorTest {

    private FileInboundConnector myConnector;
    private FileInboundConnectorService fileInboundConnectorService;
    private FileStorageService storageService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        fileInboundConnectorService = mock(FileInboundConnectorService.class);
        storageService = mock(FileStorageService.class);
        myConnector = new FileInboundConnector(fileInboundConnectorService);
        context = new Context();
        ContextRuntimeData runtimeData = new ContextRuntimeData();
        runtimeData.setWorkflowId("wf1");
        runtimeData.setWorkflowNodeKey("node1");
        context.setRuntimeData(runtimeData);
        emitter = new TestEmitter();
    }

    @Test
    void testRun_RegistersListenerAndHandlesFileCallback() throws Exception {
        // Configure component
        ComponentRuntimeConfig config = mock(ComponentRuntimeConfig.class);
        when(config.varString("output")).thenReturn(Optional.of("fileResource"));
        when(config.varString("storage")).thenReturn(Optional.of("test-storage"));
        when(fileInboundConnectorService.getFileStorageService("test-storage")).thenReturn(storageService);
        myConnector.configure(config);

        // Run
        myConnector.run(context, emitter);

        // Verify listener registered
        ArgumentCaptor<FileInboundConnectorService.IngestKey> keyCaptor = ArgumentCaptor.forClass(FileInboundConnectorService.IngestKey.class);
        ArgumentCaptor<Consumer<MultipartFile>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(fileInboundConnectorService).addListener(keyCaptor.capture(), listenerCaptor.capture());

        FileInboundConnectorService.IngestKey key = keyCaptor.getValue();
        assertEquals("wf1", key.workflowKey());
        assertEquals("node1", key.workflowNodeKey());

        // Prepare storage mock
        String fileContent = "test file content";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        when(storageService.write(anyList())).thenAnswer(invocation -> {
             List<String> keys = invocation.getArgument(0);
             return Map.of(keys.get(0), outputStream);
        });

        // Simulate callback
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");

        listenerCaptor.getValue().accept(multipartFile);

        // Verify storage write
        assertEquals(fileContent, outputStream.toString(StandardCharsets.UTF_8));

        // Verify context updated
        Optional<ContextResourceReference> refOpt = context.resource("fileResource");
        assertTrue(refOpt.isPresent());
        ContextResourceReference ref = refOpt.get();
        assertNotNull(ref);
        assertEquals("test-storage", ref.getStorage());
        assertEquals("test.txt", ref.getName());
        assertEquals("text/plain", ref.getMime());

        // Verify success emitted
        assertTrue(emitter.successCalled);

        // Verify cleanup
//        verify(fileInboundConnectorService).removeListener(eq(key));
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        boolean failureCalled = false;
        
        @Override
        public void success(Context updatedContext) {
            successCalled = true;
        }

        @Override
        public void failure(Exception ex) {
            failureCalled = true;
            ex.printStackTrace();
        }

        @Override
        public ComponentUpdateEmitter.Logger logger() {
            return (message, level, throwable) -> {
              // no-op for test
            };
        }

        @Override
        public InteractionLogger interactionLogger() {
            return (Object content,
                    String actor,
                    WorkflowInteraction.InteractionType type,
                    WorkflowInteraction.Visibility visibility) ->  System.out.println("[" + actor + "] " + content);

        }
    }
}
