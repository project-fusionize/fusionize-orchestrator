package dev.fusionize.workflow.component;

import dev.fusionize.workflow.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WorkflowComponentRegistry {
    private final ConcurrentHashMap<String, WorkflowComponentRuntime> registry = new ConcurrentHashMap<>();

    /**
     * Registers a workflow component runtime with the registry.
     *
     * @param component The component metadata
     * @param config The component configuration
     * @param runtime The runtime implementation
     * @return The registration key (format: TYPE:domain:configHash)
     */
    public String register(WorkflowComponent component,
                           WorkflowComponentConfig config,
                           WorkflowComponentRuntime runtime) {
        if (component == null || runtime == null) {
            throw new IllegalArgumentException("Component and runtime cannot be null");
        }

        String key = buildRegistryKey(component, config);
        registry.put(key, runtime);
        return key;
    }

    /**
     * Registers a workflow component runtime with the registry using a prefix.
     * The prefix should be in format "TYPE:domain" (e.g., "TASK:com.example.sendEmail")
     *
     * @param prefix The key prefix (TYPE:domain)
     * @param config The component configuration
     * @param runtime The runtime implementation
     * @return The registration key (format: TYPE:domain:configHash)
     */
    public String register(String prefix,
                           WorkflowComponentConfig config,
                           WorkflowComponentRuntime runtime) {
        if (prefix == null || runtime == null) {
            throw new IllegalArgumentException("Prefix and runtime cannot be null");
        }

        int configHash = calculateConfigHash(config);
        String key = prefix + ":" + configHash;
        registry.put(key, runtime);
        return key;
    }

    /**
     * Retrieves a workflow component runtime by prefix and config.
     * The prefix should be in format "TYPE:domain" (e.g., "TASK:com.example.sendEmail")
     *
     * @param prefix The key prefix (TYPE:domain)
     * @param config The component configuration
     * @return Optional containing the runtime if found
     */
    public Optional<WorkflowComponentRuntime> get(String prefix, WorkflowComponentConfig config) {
        int configHash = calculateConfigHash(config);
        String fullKey = prefix + ":" + configHash;
        return get(fullKey);
    }

    /**
     * Retrieves a workflow component runtime by key.
     *
     * @param key The registry key
     * @return Optional containing the runtime if found
     */
    public Optional<WorkflowComponentRuntime> get(String key) {
        return Optional.ofNullable(registry.get(key.toLowerCase()));
    }

    /**
     * Retrieves a workflow component runtime by its components.
     *
     * @param component The component metadata
     * @param config The component configuration
     * @return Optional containing the runtime if found
     */
    public Optional<WorkflowComponentRuntime> get(WorkflowComponent component,
                                                  WorkflowComponentConfig config) {
        String key = buildRegistryKey(component, config);
        return get(key);
    }

    /**
     * Finds all runtimes matching a specific node type and domain.
     *
     * @param nodeType The workflow node type
     * @param domain The component domain
     * @return List of matching runtimes
     */
    public List<WorkflowComponentRuntime> findByTypeAndDomain(WorkflowNodeType nodeType,
                                                              String domain) {
        String prefix = nodeType.getName() + ":" + domain + ":";
        return registry.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Finds all runtimes compatible with a specific node type.
     *
     * @param nodeType The workflow node type
     * @return List of matching runtimes
     */
    public List<WorkflowComponentRuntime> findByType(WorkflowNodeType nodeType) {
        String prefix = nodeType.getName() + ":";
        return registry.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Unregisters a component by key.
     *
     * @param key The registry key
     * @return true if the component was removed, false otherwise
     */
    public boolean unregister(String key) {
        return registry.remove(key) != null;
    }

    /**
     * Unregisters a component by its components.
     *
     * @param component The component metadata
     * @param config The component configuration
     * @return true if the component was removed, false otherwise
     */
    public boolean unregister(WorkflowComponent component, WorkflowComponentConfig config) {
        String key = buildRegistryKey(component, config);
        return unregister(key);
    }

    /**
     * Checks if a component is registered.
     *
     * @param key The registry key
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String key) {
        return registry.containsKey(key);
    }


    /**
     * Clears all registrations.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * Gets the number of registered components.
     *
     * @return The count of registered components
     */
    public int size() {
        return registry.size();
    }

    /**
     * Builds the registry key in format: TYPE:domain:configHash
     * Example: task:com.example.sendEmail:1234123
     */
    private String buildRegistryKey(WorkflowComponent component, WorkflowComponentConfig config) {
        WorkflowNodeType nodeType = component.getCompatible();
        String domain = component.getDomain();
        int configHash = calculateConfigHash(config);

        return String.format("%s:%s:%d",
                nodeType.getName().toLowerCase(),
                domain.toLowerCase(),
                configHash);
    }

    /**
     * Calculates hash code for the configuration.
     */
    private int calculateConfigHash(WorkflowComponentConfig config) {
        if (config == null || config.getConfig() == null) {
            return 0;
        }
        return config.getConfig().hashCode();
    }
}
