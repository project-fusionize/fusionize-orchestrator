package dev.fusionize.web;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class FileInboundConnector implements ComponentRuntime {
    private final FileInboundConnectorService fileInboundConnectorService;
    private FileStorageService fileStorageService;

    private String outputKey = "file";
    private String storage;

    public FileInboundConnector(FileInboundConnectorService fileInboundConnectorService) {
        this.fileInboundConnectorService = fileInboundConnectorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString("output").ifPresent(s -> this.outputKey = s);
        config.varString("storage").ifPresent(s -> this.storage = s);
        if (storage != null && !storage.isEmpty()) {
            this.fileStorageService = fileInboundConnectorService.getFileStorageService(storage);
        }
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (fileStorageService == null) {
            emitter.failure(new IllegalStateException("FileStorageService not found for storage: " + this.storage));
            return;
        }
        emitter.logger().info("File inbound connector activated, storing in -> {}", this.storage);
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
        emitter.logger().info("Registering file listener for key: {}", key);
        fileInboundConnectorService.addListener(key, file -> {
            emitter.logger().info("File listener triggered for key: {}", key);
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

                    context.set(outputKey, storageReference);
                    emitter.success(context);
                } else {
                    emitter.failure(new IllegalStateException("Failed to obtain output stream for key: " + randomKey));
                }

            } catch (Exception e) {
                emitter.logger().error("Error processing file webhook", e);
                emitter.failure(e);
            }
        });
    }
}
