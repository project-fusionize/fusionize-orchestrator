package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.context.ContextResourceReference;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import dev.fusionize.common.parser.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MockDataExtractorComponent implements ComponentRuntime {
    private final FileStorageService storageService;
    private String resource;
    private String mountAsData;

    public MockDataExtractorComponent(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.resource = config.varString("resource").orElse(null);
        this.mountAsData = config.varString("mountAsData").orElse(null);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {

        boolean hasResource = context.getResources().containsKey(resource);
        if (hasResource) {
            emitter.logger().info("MockDataExtractorComponent activated");
            emitter.success(context);
        } else {
            emitter.logger().info("MockDataExtractorComponent not activated");
            emitter.failure(new Exception("No resource to process"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Optional<ContextResourceReference> optionalRef = context.resource(resource);
            // emitter.logger().info("FILE REF: {}", optionalRef.orElse(null));
            if (optionalRef.isPresent() && storageService.getStorageName().equals(optionalRef.get().getStorage())) {
                Map<String, InputStream> read = storageService.read(List.of(optionalRef.get().getReferenceKey()));
                InputStream is = read.get(optionalRef.get().getReferenceKey());
                if (is != null) {
                    try (is) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, Object> map = JsonParser.MAP.fromJson(content, Map.class);
                        if (map != null && map.containsKey("extractMe")) {
                            Object value = map.get("extractMe");
                            if (value instanceof String extractedValue) {
                                context.set(this.mountAsData, extractedValue);
                                emitter.logger().info("Extracted: {}", extractedValue);
                            }
                        }
                    } catch (IOException e) {
                        emitter.failure(e);
                        return;
                    }
                }
            }
            Thread.sleep(10);

            emitter.success(context);
        } catch (InterruptedException e) {
            emitter.failure(e);
        }
    }

}