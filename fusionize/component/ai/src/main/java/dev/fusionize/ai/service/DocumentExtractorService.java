package dev.fusionize.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.ContextResourceReference;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentExtractorService {
    private static final Logger defaultLogger = LoggerFactory.getLogger(DocumentExtractorService.class);
    private final ChatClient chatClient;
    private final StorageConfigManager configManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record Response(Map<String, Object> data) implements Serializable {
    }

    private record DocumentContent(byte[] bytes, String text) {
    }

    public DocumentExtractorService(ChatClient.Builder chatClientBuilder,
                                    StorageConfigManager configManager) {
        this.chatClient = chatClientBuilder.build();
        this.configManager = configManager;
    }

    public FileStorageService getFileStorageService(String storageDomain) {
        Optional<StorageConfig> configOptional =  this.configManager.getConfig(storageDomain);
        return configOptional.map(this.configManager::getFileStorageService).orElse(null);
    }

    public Response extract(Context context, String inputVar, Map<String, Object> example) throws Exception {
        Object documentObj = resolveDocumentObject(context, inputVar);

        if (documentObj == null) {
            throw new IllegalArgumentException("Input '" + inputVar + "' not found in context (data or resource)");
        }

        DocumentContent content = parseDocumentContent(documentObj);
        String exampleJson = objectMapper.writeValueAsString(new Response(example));

        return extractDataFromDocument(content, exampleJson);
    }

    private Object resolveDocumentObject(Context context, String inputVar) {
        if (context == null || inputVar == null) {
            return null;
        }

        // 1. Try resolving from Context Resources
        Object resourceContent = tryReadFromResource(context, inputVar);
        if (resourceContent != null) {
            return resourceContent;
        }

        // 2. Fallback to Context Data
        return context.getData().get(inputVar);
    }

    private Object tryReadFromResource(Context context, String inputVar) {
        Optional<ContextResourceReference> resourceRefOpt = context.resource(inputVar);
        if (resourceRefOpt.isEmpty()) {
            return null;
        }

        ContextResourceReference ref = resourceRefOpt.get();
        if (ref.getStorage()==null || ref.getStorage().isEmpty()) {
            return null;
        }

        FileStorageService storageService = getFileStorageService(ref.getStorage());
        try {
            return readContentFromStorage(storageService, ref.getReferenceKey());
        } catch (Exception e) {
            defaultLogger.warn("Failed to read resource reference '{}': {}", ref.getReferenceKey(), e.getMessage());
            return null;
        }
    }

    private byte[] readContentFromStorage(FileStorageService storageService, String referenceKey) throws IOException {
        Map<String, InputStream> streams = storageService.read(List.of(referenceKey));
        InputStream is = streams.get(referenceKey);

        if (is != null) {
            try (is) {
                return is.readAllBytes();
            }
        }
        return null;
    }

    private DocumentContent parseDocumentContent(Object documentObj) {
        if (documentObj instanceof byte[]) {
            return new DocumentContent((byte[]) documentObj, null);
        } else if (documentObj instanceof String text) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(text);
                return new DocumentContent(decoded, null);
            } catch (IllegalArgumentException e) {
                // Not a base64 string, treat as plain text
                return new DocumentContent(null, text);
            }
        } else {
            throw new IllegalArgumentException("Unsupported document type: " + documentObj.getClass().getName());
        }
    }

    private Response extractDataFromDocument(DocumentContent content, String exampleJson) {
        if (content.bytes != null) {
            MimeType mimeType = guessMimeType(content.bytes);
            defaultLogger.info("Extracting document from {} bytes: mimeType {}", content.bytes.length, mimeType);
            return chatClient.prompt()
                    .user(u -> u.text(
                            "Check this file and extract this json out of it. Here is the example JSON structure: {example}")
                            .param("example", exampleJson)
                            .media(mimeType, new ByteArrayResource(content.bytes)))
                    .call()
                    .entity(Response.class);
        } else {
            String text = content.text;
            defaultLogger.info("Extracting document from text: {}",
                    text.substring(0, Math.min(text.length(), 50)) + "...");
            return chatClient.prompt()
                    .user(u -> u.text(
                            "Check this text and extract this json out of it. Here is the example JSON structure: {example}\n\nText content:\n{text}")
                            .param("example", exampleJson)
                            .param("text", text))
                    .call()
                    .entity(Response.class);
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
