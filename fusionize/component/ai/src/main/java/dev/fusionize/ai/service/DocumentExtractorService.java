package dev.fusionize.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.advisors.ComponentLogAdvisor;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
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
    private final StorageConfigManager configManager;
    private final AgentConfigManager agentConfigManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record Response(Map<String, Object> data) implements Serializable {
    }

    public record ExtractionPackage(
            Context context,
            String inputVar,
            Map<String, Object> example,
            String agent,
            ComponentUpdateEmitter.Logger logger,
            ComponentUpdateEmitter.InteractionLogger interactionLogger) {
    }

    private record DocumentContent(byte[] bytes, String text) {
    }

    public DocumentExtractorService(StorageConfigManager configManager,
                                    AgentConfigManager agentConfigManager) {
        this.configManager = configManager;
        this.agentConfigManager = agentConfigManager;
    }

    public FileStorageService getFileStorageService(String storageDomain) {
        Optional<StorageConfig> configOptional =  this.configManager.getConfig(storageDomain);
        return configOptional.map(this.configManager::getFileStorageService).orElse(null);
    }

    public Response extract(ExtractionPackage pkg) throws Exception {
        Object documentObj = resolveDocumentObject(pkg);

        if (documentObj == null) {
            throw new IllegalArgumentException("Input '" + pkg.inputVar() + "' not found in context (data or resource)");
        }

        DocumentContent content = parseDocumentContent(documentObj);
        String exampleJson = objectMapper.writeValueAsString(new Response(pkg.example()));
        return extractDataFromDocument(content, exampleJson, pkg);
    }

    private Object resolveDocumentObject(ExtractionPackage pkg) {
        if (pkg.context == null || pkg.inputVar == null) {
            return null;
        }

        // 1. Try resolving from Context Resources
        Object resourceContent = tryReadFromResource(pkg);
        if (resourceContent != null) {
            return resourceContent;
        }

        // 2. Fallback to Context Data
        return pkg.context.getData().get(pkg.inputVar);
    }

    private Object tryReadFromResource(ExtractionPackage pkg) {
        Optional<ContextResourceReference> resourceRefOpt = pkg.context.resource(pkg.inputVar);
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
            if (pkg.logger != null) {
                pkg.logger.warn("Failed to read resource reference '{}': {}", ref.getReferenceKey(), e.getMessage());
            }else{
                defaultLogger.warn("Failed to read resource reference '{}': {}", ref.getReferenceKey(), e.getMessage());
            }
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

    private Response extractDataFromDocument(DocumentContent content,
                                             String exampleJson,
                                             ExtractionPackage pkg) throws AgentConfigNotFoundException, ChatModelException {
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent);
        if (content.bytes != null) {
            MimeType mimeType = guessMimeType(content.bytes);
            if (pkg.logger != null) {
                pkg.logger.info("Extracting document from {} bytes: mimeType {}", content.bytes.length, mimeType);
            }else {
                defaultLogger.info("Extracting document from {} bytes: mimeType {}", content.bytes.length, mimeType);
            }
            return chatClient.prompt()
                    .user(u -> u.text(
                            "Check this file and extract this json out of it. Here is the example JSON structure: {example}")
                            .param("example", exampleJson)
                            .media(mimeType, new ByteArrayResource(content.bytes)))
                    .advisors(new ComponentLogAdvisor(pkg.interactionLogger))
                    .call()
                    .entity(Response.class);
        } else {
            String text = content.text;
            String textSubstring = text.substring(0, Math.min(text.length(), 50));
            if (pkg.logger !=  null) {
                pkg.logger.info("Extracting document from text: {}...",textSubstring);
            }else {
                defaultLogger.info("Extracting document from text: {}...",textSubstring);
            }
            return chatClient.prompt()
                    .user(u -> u.text(
                            "Check this text and extract this json out of it. Here is the example JSON structure: {example}\n\nText content:\n{text}")
                            .param("example", exampleJson)
                            .param("text", text))
                    .advisors(new ComponentLogAdvisor(pkg.interactionLogger))
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
