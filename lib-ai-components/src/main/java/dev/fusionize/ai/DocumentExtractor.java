package dev.fusionize.ai;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.apache.tika.Tika;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentExtractor implements ComponentRuntime {
    public record Response (Map<String,Object> data) implements Serializable {}
    private static final Logger logger = LoggerFactory.getLogger(DocumentExtractor.class);

    public static final String CONF_INPUT_VAR = "inputVar";
    public static final String CONF_OUTPUT_VAR = "outputVar";
    public static final String CONF_EXAMPLE = "example";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String inputVar = "document";
    private String outputVar = "extractedData";
    private Map<String, Object> example = new HashMap<>();

    public DocumentExtractor(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varMap(CONF_EXAMPLE).ifPresent(m -> this.example = m);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (context.contains(inputVar)) {
            emitter.success(context);
        } else {
            emitter.failure(new IllegalArgumentException("Input variable '" + inputVar + "' not found in context"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Object documentObj = context.getData().get(inputVar);
            if (documentObj == null) {
                emitter.failure(new IllegalArgumentException("Document content is null"));
                return;
            }

            byte[] documentBytes = null;
            String documentText = null;

            if (documentObj instanceof byte[]) {
                documentBytes = (byte[]) documentObj;
            } else if (documentObj instanceof String) {
                String text = (String) documentObj;
                try {
                    documentBytes = java.util.Base64.getDecoder().decode(text);
                } catch (IllegalArgumentException e) {
                    // Not a base64 string, treat as plain text
                    documentText = text;
                }
            } else {
                emitter.failure(new IllegalArgumentException("Unsupported document type: " + documentObj.getClass().getName()));
                return;
            }

            String exampleJson = objectMapper.writeValueAsString(new Response(example));

            Response response;
            if (documentBytes != null) {
                MimeType mimeType = guessMimeType(documentBytes);
                emitter.logger().info("Extracting document from {} bytes: mimeType {}", documentBytes.length, mimeType);
                byte[] finalDocumentBytes = documentBytes;
                response = chatClient.prompt()
                        .user(u -> u.text("Check this file and extract this json out of it. Here is the example JSON structure: {example}")
                                .param("example", exampleJson)
                                .media(mimeType, new ByteArrayResource(finalDocumentBytes)))
                        .call()
                        .entity(Response.class);
            } else {
                String finalDocumentText = documentText;
                emitter.logger().info("Extracting document from text: {}", finalDocumentText.substring(0, Math.min(finalDocumentText.length(), 50)) + "...");
                response = chatClient.prompt()
                        .user(u -> u.text("Check this text and extract this json out of it. Here is the example JSON structure: {example}\n\nText content:\n{text}")
                                .param("example", exampleJson)
                                .param("text", finalDocumentText))
                        .call()
                        .entity(Response.class);
            }

            if(response== null){
                throw new Exception("response is null");
            }
            context.getData().put(outputVar, response.data);
            emitter.success(context);


        } catch (Exception e) {
            emitter.logger().error("Error extracting document data", e);
            emitter.failure(e);
        }
    }

    private MimeType guessMimeType(byte[] data) {
        if (data == null || data.length == 0) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }

        Tika tika = new Tika();
        String detectedType = tika.detect(data);
        
        if (detectedType != null) {
            try {
                return MimeType.valueOf(detectedType);
            } catch (Exception e) {
                // Ignore invalid mime types
            }
        }

        return MimeTypeUtils.APPLICATION_OCTET_STREAM;
    }
}
