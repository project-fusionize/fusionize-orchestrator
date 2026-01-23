package dev.fusionize.ai.model.descriptor;

import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.common.parser.descriptor.Description;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentConfigDescription extends Description {
    private String name;
    private String domain;
    private String description;
    private List<String> tags = new ArrayList<>();
    private String modelConfigDomain;
    private String instructionPrompt;
    private List<String> allowedMcpTools = new ArrayList<>();
    private AgentConfig.Role role;
    private Map<String, Object> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getModelConfigDomain() {
        return modelConfigDomain;
    }

    public void setModelConfigDomain(String modelConfigDomain) {
        this.modelConfigDomain = modelConfigDomain;
    }

    public String getInstructionPrompt() {
        return instructionPrompt;
    }

    public void setInstructionPrompt(String instructionPrompt) {
        this.instructionPrompt = instructionPrompt;
    }

    public List<String> getAllowedMcpTools() {
        return allowedMcpTools;
    }

    public void setAllowedMcpTools(List<String> allowedMcpTools) {
        this.allowedMcpTools = allowedMcpTools;
    }

    public AgentConfig.Role getRole() {
        return role;
    }

    public void setRole(AgentConfig.Role role) {
        this.role = role;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
