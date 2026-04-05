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
    private static final Logger log = LoggerFactory.getLogger(DocumentExtractorService.class);
    private static final Tika TIKA = new Tika();

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
        if (pkg.context() == null || pkg.inputVar() == null) {
            return null;
        }

        Object resourceContent = tryReadFromResource(pkg);
        if (resourceContent != null) {
            return resourceContent;
        }

        return pkg.context().getData().get(pkg.inputVar());
    }

    private Object tryReadFromResource(ExtractionPackage pkg) {
        Optional<ContextResourceReference> resourceRefOpt = pkg.context().resource(pkg.inputVar());
        if (resourceRefOpt.isEmpty()) {
            return null;
        }

        ContextResourceReference ref = resourceRefOpt.get();
        if (ref.getStorage() == null || ref.getStorage().isEmpty()) {
            return null;
        }

        FileStorageService storageService = getFileStorageService(ref.getStorage());
        if (storageService == null) {
            return null;
        }

        try {
            return readContentFromStorage(storageService, ref.getReferenceKey());
        } catch (Exception e) {
            logWarn(pkg.logger(), "Failed to read resource reference '{}': {}", ref.getReferenceKey(), e.getMessage());
            return null;
        }
    }

    private FileStorageService getFileStorageService(String storageDomain) {
        Optional<StorageConfig> configOptional = this.configManager.getConfig(storageDomain);
        return configOptional.map(this.configManager::getFileStorageService).orElse(null);
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
        if (documentObj instanceof byte[] bytes) {
            return new DocumentContent(bytes, null);
        } else if (documentObj instanceof String text) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(text);
                return new DocumentContent(decoded, null);
            } catch (IllegalArgumentException e) {
                return new DocumentContent(null, text);
            }
        } else {
            throw new IllegalArgumentException("Unsupported document type: " + documentObj.getClass().getName());
        }
    }

    private Response extractDataFromDocument(DocumentContent content,
                                             String exampleJson,
                                             ExtractionPackage pkg) throws AgentConfigNotFoundException, ChatModelException {
        ChatClient chatClient = this.agentConfigManager.getChatClient(pkg.agent());
        if (content.bytes() != null) {
            MimeType mimeType = guessMimeType(content.bytes());
            logInfo(pkg.logger(), "Extracting document from {} bytes: mimeType {}", content.bytes().length, mimeType);
            return chatClient.prompt()
                    .user(u -> u.text(
                                    "Check this file and extract this json out of it. Here is the example JSON structure: {example}")
                            .param("example", exampleJson)
                            .media(mimeType, new ByteArrayResource(content.bytes())))
                    .advisors(new ComponentLogAdvisor(pkg.interactionLogger()))
                    .call()
                    .entity(Response.class);
        } else {
            String text = content.text();
            String textPreview = text.substring(0, Math.min(text.length(), 50));
            logInfo(pkg.logger(), "Extracting document from text: {}...", textPreview);
            return chatClient.prompt()
                    .user(u -> u.text(
                                    "Check this text and extract this json out of it. Here is the example JSON structure: {example}\n\nText content:\n{text}")
                            .param("example", exampleJson)
                            .param("text", text))
                    .advisors(new ComponentLogAdvisor(pkg.interactionLogger()))
                    .call()
                    .entity(Response.class);
        }
    }

    private MimeType guessMimeType(byte[] data) {
        if (data == null || data.length == 0) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }

        String detectedType = TIKA.detect(data);
        if (detectedType != null) {
            try {
                return MimeType.valueOf(detectedType);
            } catch (Exception e) {
                // Ignore invalid mime types
            }
        }

        return MimeTypeUtils.APPLICATION_OCTET_STREAM;
    }

    private void logInfo(ComponentUpdateEmitter.Logger logger, String message, Object... args) {
        if (logger != null) {
            logger.info(message, args);
        } else {
            log.info(message, args);
        }
    }

    private void logWarn(ComponentUpdateEmitter.Logger logger, String message, Object... args) {
        if (logger != null) {
            logger.warn(message, args);
        } else {
            log.warn(message, args);
        }
    }
}
