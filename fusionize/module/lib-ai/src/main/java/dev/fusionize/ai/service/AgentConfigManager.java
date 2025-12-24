package dev.fusionize.ai.service;

import dev.fusionize.ai.exception.*;
import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.repo.AgentConfigRepository;
import dev.fusionize.ai.repo.AgentConfigRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class AgentConfigManager {

    private final AgentConfigRepository repository;
    private final ChatModelManager chatModelManager;
    private final McpManager mcpManager;

    public AgentConfigManager(AgentConfigRepository repository,
                              ChatModelManager chatModelManager,
                              McpManager mcpManager) {
        this.repository = repository;
        this.chatModelManager = chatModelManager;
        this.mcpManager = mcpManager;
    }

    public AgentConfig saveConfig(AgentConfig config) throws AgentConfigException {
        validateConfig(config);

        // Check availability of domain
        // For new records
        if (config.getId() == null && repository.findByDomain(config.getDomain()).isPresent()) {
            throw new AgentConfigDomainAlreadyExistsException(config.getDomain());
        }

        // For updates, ensure we are not changing the domain to one that already exists on another record
        if (config.getId() != null) {
            Optional<AgentConfig> existing = repository.findByDomain(config.getDomain());
            if (existing.isPresent() && !existing.get().getId().equals(config.getId())) {
                throw new AgentConfigDomainAlreadyExistsException(config.getDomain());
            }
        }

        return repository.save(config);
    }

    public Optional<AgentConfig> getConfig(String domain) {
        return repository.findByDomain(domain);
    }

    public void deleteConfig(String domain) {
        repository.deleteByDomain(domain);
    }

    public List<AgentConfig> getAll(String domain) {
        return repository.findByDomainStartingWith(domain);
    }

    private void validateConfig(AgentConfig config) throws InvalidAgentConfigException {
        if (config == null) {
            throw new InvalidAgentConfigException("Config cannot be null");
        }
        if (!StringUtils.hasText(config.getDomain())) {
            throw new InvalidAgentConfigException("Domain is required");
        }
        
        // Validate modelConfigDomain references a valid ChatModelConfig
        if (StringUtils.hasText(config.getModelConfigDomain())) {
            if (chatModelManager.getModel(config.getModelConfigDomain()).isEmpty()) {
                throw new InvalidAgentConfigException("Referenced ChatModelConfig domain not found: " + config.getModelConfigDomain());
            }
        }

        // Validate allowedMcpTools refer to valid McpClientConfig domains
        if (config.getAllowedMcpTools() != null && !config.getAllowedMcpTools().isEmpty()) {
            for (String toolDomain : config.getAllowedMcpTools()) {
                if (mcpManager.getConfigByDomain(toolDomain).isEmpty()) {
                    throw new InvalidAgentConfigException("Referenced McpClientConfig domain not found: " + toolDomain);
                }
            }
        }
    }

    public ChatClient getChatClient(String domain) throws AgentConfigNotFoundException, ChatModelException {
        Optional<AgentConfig> configOptional = getConfig(domain);
        if (configOptional.isEmpty()) {
            throw new AgentConfigNotFoundException("No config found for domain: " + domain);
        }
        AgentConfig aConfig = configOptional.get();
        ChatClient defaultClient = chatModelManager.getChatClient(aConfig.getModelConfigDomain());
        return defaultClient.mutate()
                .defaultSystem(aConfig.getInstructionPrompt()).build();
    }
}
