package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.context.ContextResourceReference;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Start component that waits for a file input stream via a callback method.
 * When a file arrives, it triggers the workflow chain.
 */
public final class MockFileHandlerComponent implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MockFileHandlerComponent.class);
    private final FileStorageService storageService;

    private final BlockingQueue<InputStream> fileQueue;
    private String mountAsResource;

    public MockFileHandlerComponent(FileStorageService storageService, BlockingQueue<InputStream> fileQueue) {
        this.storageService = storageService;
        this.fileQueue = fileQueue;
    }


    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.mountAsResource = config.varString("mountAsResource").orElse(null);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(100);
            emitter.logger().info("MockFileHandlerComponent activated");
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        while (true) {
            try {
                // Block until a file is available
                InputStream inputStream = fileQueue.take();
                String randomKey = KeyUtil.getFlatUUID();
                Map<String, OutputStream> writeMap = storageService.write(List.of(randomKey));
                OutputStream outputStream = writeMap.get(randomKey);
                if (outputStream != null) {
                    try (inputStream; outputStream) {
                        inputStream.transferTo(outputStream);
                    }
                    ContextResourceReference storageReference = ContextResourceReference.builder()
                            .withMime(MimeTypeUtils.APPLICATION_JSON.toString())
                            .withStorage(storageService.getStorageName())
                            .withReferenceKey(randomKey)
                            .withName(randomKey + ".json")
                            .build();

                    context.set(mountAsResource, storageReference);
                    emitter.logger().info("MockFileHandlerComponent handling file stream");
                    emitter.success(context);
                }


            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for file", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                emitter.logger().error("Error processing file", e);
                emitter.failure(e);
                logger.error("Error processing file", e);
            }
        }
    }

}