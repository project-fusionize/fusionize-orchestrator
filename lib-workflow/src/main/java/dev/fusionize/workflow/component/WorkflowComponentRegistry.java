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
    private final ConcurrentHashMap<String, WorkflowComponentFactory> factoryRegistry = new ConcurrentHashMap<>();

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
     * Registers a workflow component factory with the registry.
     * The prefix should be in format "TYPE:domain" (e.g., "TASK:com.example.sendEmail")
     *
     * @param prefix The key prefix (TYPE:domain)
     * @param factory The factory for creating component instances
     */
    public void registerFactory(String prefix, WorkflowComponentFactory factory) {
        if (prefix == null || factory == null) {
            throw new IllegalArgumentException("Prefix and factory cannot be null");
        }
        factoryRegistry.put(prefix.toLowerCase(), factory);
    }

    /**
     * Registers a workflow component factory with the registry using component metadata.
     *
     * @param component The component metadata
     * @param factory The factory for creating component instances
     */
    public void registerFactory(WorkflowComponent component, WorkflowComponentFactory factory) {
        if (component == null || factory == null) {
            throw new IllegalArgumentException("Component and factory cannot be null");
        }
        String prefix = buildPrefix(component);
        registerFactory(prefix, factory);
    }

    /**
     * Retrieves a workflow component runtime by prefix and config.
     * The prefix should be in format "TYPE:domain" (e.g., "TASK:com.example.sendEmail")
     * If not found in registry, attempts to create from factory.
     *
     * @param prefix The key prefix (TYPE:domain)
     * @param config The component configuration
     * @return Optional containing the runtime if found or created
     */
    public Optional<WorkflowComponentRuntime> get(String prefix, WorkflowComponentConfig config) {
        int configHash = calculateConfigHash(config);
        String fullKey = prefix + ":" + configHash;

        // First try to get from registry
        Optional<WorkflowComponentRuntime> runtime = get(fullKey);
        if (runtime.isPresent()) {
            return runtime;
        }

        // If not found, try to create from factory
        return createFromFactory(prefix, config);
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
     * If not found in registry, attempts to create from factory.
     *
     * @param component The component metadata
     * @param config The component configuration
     * @return Optional containing the runtime if found or created
     */
    public Optional<WorkflowComponentRuntime> get(WorkflowComponent component,
                                                  WorkflowComponentConfig config) {
        String key = buildRegistryKey(component, config);

        // First try to get from registry
        Optional<WorkflowComponentRuntime> runtime = get(key);
        if (runtime.isPresent()) {
            return runtime;
        }

        // If not found, try to create from factory
        String prefix = buildPrefix(component);
        return createFromFactory(prefix, config);
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
        factoryRegistry.clear();
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
     * Gets the number of registered factories.
     *
     * @return The count of registered factories
     */
    public int factoryCount() {
        return factoryRegistry.size();
    }

    /**
     * Checks if a factory is registered for the given prefix.
     *
     * @param prefix The key prefix (TYPE:domain)
     * @return true if factory is registered, false otherwise
     */
    public boolean hasFactory(String prefix) {
        return factoryRegistry.containsKey(prefix.toLowerCase());
    }

    /**
     * Checks if a factory is registered for the given component.
     *
     * @param component The component metadata
     * @return true if factory is registered, false otherwise
     */
    public boolean hasFactory(WorkflowComponent component) {
        String prefix = buildPrefix(component);
        return hasFactory(prefix);
    }

    /**
     * Unregisters a factory by prefix.
     *
     * @param prefix The key prefix (TYPE:domain)
     * @return true if the factory was removed, false otherwise
     */
    public boolean unregisterFactory(String prefix) {
        return factoryRegistry.remove(prefix.toLowerCase()) != null;
    }

    /**
     * Unregisters a factory by component.
     *
     * @param component The component metadata
     * @return true if the factory was removed, false otherwise
     */
    public boolean unregisterFactory(WorkflowComponent component) {
        String prefix = buildPrefix(component);
        return unregisterFactory(prefix);
    }

    /**
     * Clears all factory registrations.
     */
    public void clearFactories() {
        factoryRegistry.clear();
    }

    /**
     * Creates a component runtime from factory if available.
     *
     * @param prefix The component prefix
     * @param config The component configuration
     * @return Optional containing the created runtime if factory exists
     */
    private Optional<WorkflowComponentRuntime> createFromFactory(String prefix, WorkflowComponentConfig config) {
        WorkflowComponentFactory factory = factoryRegistry.get(prefix.toLowerCase());
        if (factory != null) {
            try {
                WorkflowComponentRuntime runtime = factory.create();
                // Optionally register the created runtime for future use
                int configHash = calculateConfigHash(config);
                String key = prefix + ":" + configHash;
                runtime.configure(config);
                registry.put(key, runtime);
                return Optional.of(runtime);
            } catch (Exception e) {
                // Log error if needed, but don't fail the request
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Builds the prefix from component metadata.
     *
     * @param component The component metadata
     * @return The prefix in format "TYPE:domain"
     */
    private String buildPrefix(WorkflowComponent component) {
        WorkflowNodeType nodeType = component.getCompatible();
        String domain = component.getDomain();
        return String.format("%s:%s", nodeType.getName().toLowerCase(), domain.toLowerCase());
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
