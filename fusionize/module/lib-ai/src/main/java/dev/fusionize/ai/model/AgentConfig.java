package dev.fusionize.ai.model;

import dev.fusionize.user.activity.DomainEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "ai_agent_config")
public class AgentConfig extends DomainEntity  {
    @Id
    private String id;
    private String description;
    private List<String> tags = new ArrayList<>();

    private String modelConfigDomain;
    private String instructionPrompt;
    private List<String> allowedMcpTools = new ArrayList<>();
    private Role role;
    private Map<String,Object> properties = new HashMap<>();


    public enum Role {
        ANALYZER,
        DECIDER,
        GENERATOR,
        VALIDATOR,
        ROUTER
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
