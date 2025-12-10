package dev.fusionize.orchestrator.ai;

import dev.fusionize.Application;
import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.ai.exception.ChatModelNotFoundException;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.service.ChatModelManager;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Application.API_PREFIX + "/chat-model-config")
public class ChatModelConfigController {

    private final ChatModelManager chatModelManager;

    public ChatModelConfigController(ChatModelManager chatModelManager) {
        this.chatModelManager = chatModelManager;
    }

    @GetMapping
    public ServicePayload<List<ChatModelConfig>> getAll(
            @RequestParam(required = false, defaultValue = "") String domain) {
        List<ChatModelConfig> configs = chatModelManager.getAll(domain).stream().map(ChatModelConfig::sanitize).toList();
        return new ServicePayload.Builder<List<ChatModelConfig>>()
                .response(new ServiceResponse.Builder<List<ChatModelConfig>>()
                        .status(200)
                        .message(configs)
                        .build())
                .build();
    }

    @GetMapping("/{domain}")
    public ServicePayload<ChatModelConfig> get(@PathVariable String domain) throws ChatModelException {
        ChatModelConfig config = chatModelManager.getModel(domain)
                .orElseThrow(() -> new ChatModelNotFoundException(domain));
        return new ServicePayload.Builder<ChatModelConfig>()
                .response(new ServiceResponse.Builder<ChatModelConfig>()
                        .status(200)
                        .message(config.sanitize())
                        .build())
                .build();
    }

    @PostMapping
    public ServicePayload<ChatModelConfig> create(@RequestBody ChatModelConfig config) throws ChatModelException {
        ChatModelConfig saved = chatModelManager.saveModel(config);
        return new ServicePayload.Builder<ChatModelConfig>()
                .response(new ServiceResponse.Builder<ChatModelConfig>()
                        .status(200)
                        .message(saved.sanitize())
                        .build())
                .build();
    }

    @PutMapping("/{domain}")
    public ServicePayload<ChatModelConfig> update(@PathVariable String domain, @RequestBody ChatModelConfig config)
            throws ChatModelException {
        ChatModelConfig existing = chatModelManager.getModel(domain)
                .orElseThrow(() -> new ChatModelNotFoundException(domain));

        // Ensure the ID and domain are preserved/updated correctly
        config.setId(existing.getId());
        config.setDomain(domain);

        ChatModelConfig saved = chatModelManager.saveModel(config);
        return new ServicePayload.Builder<ChatModelConfig>()
                .response(new ServiceResponse.Builder<ChatModelConfig>()
                        .status(200)
                        .message(saved.sanitize())
                        .build())
                .build();
    }

    @DeleteMapping("/{domain}")
    public ServicePayload<Void> delete(@PathVariable String domain) {
        chatModelManager.deleteModel(domain);
        return new ServicePayload.Builder<Void>()
                .response(new ServiceResponse.Builder<Void>()
                        .status(200)
                        .build())
                .build();
    }

    @PostMapping("/test-connection")
    public ServicePayload<Void> testConnection(@RequestBody ChatModelConfig config) throws ChatModelException {
        chatModelManager.testConnection(config);
        return new ServicePayload.Builder<Void>()
                .response(new ServiceResponse.Builder<Void>()
                        .status(200)
                        .build())
                .build();
    }
}
