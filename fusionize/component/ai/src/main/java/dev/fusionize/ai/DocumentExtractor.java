package dev.fusionize.ai;

import dev.fusionize.ai.service.DocumentExtractorService;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.HashMap;
import java.util.Map;

public class DocumentExtractor implements ComponentRuntime {

    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_EXAMPLE = "example";
    public static final String CONF_STORAGE = "storage";


    private final DocumentExtractorService documentExtractorService;
    private FileStorageService fileStorageService;

    private String inputVar = "document";
    private String outputVar = "extractedData";
    private String storage = null;

    private Map<String, Object> example = new HashMap<>();

    public DocumentExtractor(DocumentExtractorService documentExtractorService) {
        this.documentExtractorService = documentExtractorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_STORAGE).ifPresent(s -> this.storage = s);
        config.varMap(CONF_EXAMPLE).ifPresent(m -> this.example = m);
        if(this.storage!=null){
            this.fileStorageService = this.documentExtractorService.getFileStorageService(storage);
        }
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (context.contains(inputVar) || context.getResources().containsKey(inputVar)) {
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            DocumentExtractorService.Response response = documentExtractorService.extract(context, inputVar, fileStorageService, example);

            if (response == null) {
                throw new Exception("response is null");
            }
            context.getData().put(outputVar, response.data());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error extracting document data", e);
            emitter.failure(e);
        }
    }
}
