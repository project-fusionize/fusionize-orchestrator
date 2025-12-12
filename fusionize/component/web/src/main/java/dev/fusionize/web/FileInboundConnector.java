package dev.fusionize.web;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class FileInboundConnector implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(FileInboundConnector.class);
    private final FileInboundConnectorService fileInboundConnectorService;
    private FileStorageService fileStorageService;

    private String outputKey;
    private String storage;

    public FileInboundConnector(FileInboundConnectorService fileInboundConnectorService) {
        this.fileInboundConnectorService = fileInboundConnectorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.outputKey = config.varString("output").orElse(null);
        this.storage = config.varString("storage").orElse(null);
        if(storage!=null && !storage.isEmpty()){
            this.fileStorageService = fileInboundConnectorService.getFileStorageService(storage);
        }
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if(fileStorageService==null){
            emitter.failure(new Exception("FileStorageService is not found: " + this.storage ));
        }
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        String workflowKey = context.getRuntimeData().getWorkflowDomain();
        workflowKey = workflowKey == null ? context.getRuntimeData().getWorkflowId() : workflowKey;
        String workflowNodeKey = context.getRuntimeData().getWorkflowNodeKey();

        if (workflowKey == null || workflowNodeKey == null) {
            emitter.failure(new IllegalStateException("WorkflowKey and WorkflowNodeKey are required"));
            return;
        }

        FileInboundConnectorService.IngestKey key = new FileInboundConnectorService.IngestKey(workflowKey, workflowNodeKey);

        logger.info("Registering file webhook listener for key: {}", key);

        fileInboundConnectorService.addListener(key, file -> {
            logger.info("File webhook triggered for key: {}", key);
            try {
                String randomKey = KeyUtil.getFlatUUID();
                randomKey += file.getOriginalFilename();
                Map<String, OutputStream> writeMap = fileStorageService.write(List.of(randomKey));
                OutputStream outputStream = writeMap.get(randomKey);


                if (outputStream != null) {
                    try (InputStream inputStream = file.getInputStream(); outputStream) {
                        inputStream.transferTo(outputStream);
                    }
                    fileStorageService.save(writeMap.keySet().stream().toList());
                    ContextResourceReference storageReference = ContextResourceReference.builder()
                            .withMime(file.getContentType())
                            .withStorage(storage)
                            .withReferenceKey(randomKey)
                            .withName(file.getOriginalFilename())
                            .build();

                    if (outputKey != null) {
                        context.set(outputKey, storageReference);
                    }
//                    fileInboundConnectorService.removeListener(key); // Cleanup
                    emitter.success(context);
                } else {
                     emitter.failure(new IllegalStateException("Failed to obtain output stream for key: " + randomKey));
                }

            } catch (Exception e) {
                logger.error("Error processing file webhook", e);
                emitter.failure(e);
            }
        });
    }
}
