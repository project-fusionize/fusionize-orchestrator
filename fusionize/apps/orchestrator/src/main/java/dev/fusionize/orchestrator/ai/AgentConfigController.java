package dev.fusionize.orchestrator.ai;

import dev.fusionize.Application;
import dev.fusionize.ai.exception.AgentConfigException;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.service.AgentConfigManager;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Application.API_PREFIX + "/agent-config")
public class AgentConfigController {

    private final AgentConfigManager agentConfigManager;

    public AgentConfigController(AgentConfigManager agentConfigManager) {
        this.agentConfigManager = agentConfigManager;
    }

    @GetMapping
    public ServicePayload<List<AgentConfig>> getAll(
            @RequestParam(required = false, defaultValue = "") String domain) {
        List<AgentConfig> configs = agentConfigManager.getAll(domain);
        return new ServicePayload.Builder<List<AgentConfig>>()
                .response(new ServiceResponse.Builder<List<AgentConfig>>()
                        .status(200)
                        .message(configs)
                        .build())
                .build();
    }

    @GetMapping("/{domain}")
    public ServicePayload<AgentConfig> get(@PathVariable String domain) throws AgentConfigException {
        AgentConfig config = agentConfigManager.getConfig(domain)
                .orElseThrow(() -> new AgentConfigNotFoundException(domain));

        return new ServicePayload.Builder<AgentConfig>()
                .response(new ServiceResponse.Builder<AgentConfig>()
                        .status(200)
                        .message(config)
                        .build())
                .build();
    }

    @PostMapping
    public ServicePayload<AgentConfig> create(@RequestBody AgentConfig config) throws AgentConfigException {
        AgentConfig saved = agentConfigManager.saveConfig(config);
        return new ServicePayload.Builder<AgentConfig>()
                .response(new ServiceResponse.Builder<AgentConfig>()
                        .status(200)
                        .message(saved)
                        .build())
                .build();
    }

    @PutMapping("/{domain}")
    public ServicePayload<AgentConfig> update(@PathVariable String domain, @RequestBody AgentConfig config)
            throws AgentConfigException {
        AgentConfig existing = agentConfigManager.getConfig(domain)
                .orElseThrow(() -> new AgentConfigNotFoundException(domain));

        config.setId(existing.getId());
        config.setDomain(domain);

        AgentConfig saved = agentConfigManager.saveConfig(config);
        return new ServicePayload.Builder<AgentConfig>()
                .response(new ServiceResponse.Builder<AgentConfig>()
                        .status(200)
                        .message(saved)
                        .build())
                .build();
    }

    @DeleteMapping("/{domain}")
    public ServicePayload<Void> delete(@PathVariable String domain) {
        agentConfigManager.deleteConfig(domain);
        return new ServicePayload.Builder<Void>()
                .response(new ServiceResponse.Builder<Void>()
                        .status(200)
                        .build())
                .build();
    }
}
