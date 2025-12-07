package dev.fusionize.orchestrator.ai;

import dev.fusionize.Application;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.service.ChatModelManager;
import dev.fusionize.common.exception.ApplicationException;
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
            @RequestParam(required = false, defaultValue = "") String domain) throws ApplicationException {
        try {
            List<ChatModelConfig> configs = chatModelManager.getAll(domain);
            return new ServicePayload.Builder<List<ChatModelConfig>>()
                    .response(new ServiceResponse.Builder<List<ChatModelConfig>>()
                            .status(200)
                            .message(configs)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    @GetMapping("/{domain}")
    public ServicePayload<ChatModelConfig> get(@PathVariable String domain) throws ApplicationException {
        try {
            ChatModelConfig config = chatModelManager.getModel(domain)
                    .orElseThrow(() -> new ApplicationException("Config not found for domain: " + domain));
            return new ServicePayload.Builder<ChatModelConfig>()
                    .response(new ServiceResponse.Builder<ChatModelConfig>()
                            .status(200)
                            .message(config)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    @PostMapping
    public ServicePayload<ChatModelConfig> create(@RequestBody ChatModelConfig config) throws ApplicationException {
        try {
            ChatModelConfig saved = chatModelManager.saveModel(config);
            return new ServicePayload.Builder<ChatModelConfig>()
                    .response(new ServiceResponse.Builder<ChatModelConfig>()
                            .status(200)
                            .message(saved)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    @PutMapping("/{domain}")
    public ServicePayload<ChatModelConfig> update(@PathVariable String domain, @RequestBody ChatModelConfig config)
            throws ApplicationException {
        try {
            ChatModelConfig existing = chatModelManager.getModel(domain)
                    .orElseThrow(() -> new ApplicationException("Config not found for domain: " + domain));

            // Ensure the ID and domain are preserved/updated correctly
            config.setId(existing.getId());
            config.setDomain(domain);

            ChatModelConfig saved = chatModelManager.saveModel(config);
            return new ServicePayload.Builder<ChatModelConfig>()
                    .response(new ServiceResponse.Builder<ChatModelConfig>()
                            .status(200)
                            .message(saved)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    @DeleteMapping("/{domain}")
    public ServicePayload<Void> delete(@PathVariable String domain) throws ApplicationException {
        try {
            chatModelManager.deleteModel(domain);
            return new ServicePayload.Builder<Void>()
                    .response(new ServiceResponse.Builder<Void>()
                            .status(200)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }
}
