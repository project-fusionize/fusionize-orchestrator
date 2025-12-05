package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ComponentRuntimeRegistry {
    private final ConcurrentHashMap<String, ComponentRuntime> registry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComponentRuntimeFactory<?>> factoryRegistry = new ConcurrentHashMap<>();

    /**
     * Registers a workflow component runtime with the registry.
     *
     * @param component The component metadata
     * @param config    The component configuration
     * @param runtime   The runtime implementation
     * @return The registration key (format: TYPE:domain:configHash)
     */
    public String register(WorkflowComponent component,
            ComponentRuntimeConfig config,
            ComponentRuntime runtime) {
        if (component == null || runtime == null) {
            throw new IllegalArgumentException("Component and runtime cannot be null");
        }

        String lastKey = null;
        for (Actor actor : component.getActors()) {
            String key = buildRegistryKey(actor, component.getDomain(), config);
            registry.put(key, runtime);
            lastKey = key;
        }
        return lastKey; // Return the last key, or maybe change return type to List<String>? Keeping
                        // String for now to minimize breakage.
    }

    /**
     * Registers a workflow component runtime with the registry using a prefix.
     * The prefix should be in format "TYPE:domain" (e.g.,
     * "TASK:com.example.sendEmail")
     *
     * @param prefix  The key prefix (TYPE:domain)
     * @param config  The component configuration
     * @param runtime The runtime implementation
     * @return The registration key (format: TYPE:domain:configHash)
     */
    public String register(String prefix,
            ComponentRuntimeConfig config,
            ComponentRuntime runtime) {
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
     * The prefix should be in format "TYPE:domain" (e.g.,
     * "TASK:com.example.sendEmail")
     *
     * @param prefix  The key prefix (TYPE:domain)
     * @param factory The factory for creating component instances
     */
    public void registerFactory(String prefix, ComponentRuntimeFactory<?> factory) {
        if (prefix == null || factory == null) {
            throw new IllegalArgumentException("Prefix and factory cannot be null");
        }
        factoryRegistry.put(prefix.toLowerCase(), factory);
    }

    /**
     * Registers a workflow component factory with the registry using component
     * metadata.
     *
     * @param component The component metadata
     * @param factory   The factory for creating component instances
     */
    public void registerFactory(WorkflowComponent component, ComponentRuntimeFactory<?> factory) {
        if (component == null || factory == null) {
            throw new IllegalArgumentException("Component and factory cannot be null");
        }
        for (Actor actor : component.getActors()) {
            String prefix = buildPrefix(actor, component.getDomain());
            registerFactory(prefix, factory);
        }
    }

    /**
     * Retrieves a workflow component runtime by prefix and config.
     * The prefix should be in format "TYPE:domain" (e.g.,
     * "TASK:com.example.sendEmail")
     * If not found in registry, attempts to create from factory.
     *
     * @param prefix The key prefix (TYPE:domain)
     * @param config The component configuration
     * @return Optional containing the runtime if found or created
     */
    public Optional<ComponentRuntime> get(String prefix, ComponentRuntimeConfig config) {
        int configHash = calculateConfigHash(config);
        String fullKey = prefix + ":" + configHash;

        // First try to get from registry
        Optional<ComponentRuntime> runtime = get(fullKey);
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
    public Optional<ComponentRuntime> get(String key) {
        return Optional.ofNullable(registry.get(key.toLowerCase()));
    }

    /**
     * Retrieves a workflow component runtime by its components.
     * If not found in registry, attempts to create from factory.
     *
     * @param component The component metadata
     * @param config    The component configuration
     * @return Optional containing the runtime if found or created
     */
    public Optional<ComponentRuntime> get(WorkflowComponent component,
            ComponentRuntimeConfig config) {
        for (Actor actor : component.getActors()) {
            String key = buildRegistryKey(actor, component.getDomain(), config);
            Optional<ComponentRuntime> runtime = get(key);
            if (runtime.isPresent()) {
                return runtime;
            }
        }

        // If not found, try to create from factory
        for (Actor actor : component.getActors()) {
            String prefix = buildPrefix(actor, component.getDomain());
            Optional<ComponentRuntime> runtime = createFromFactory(prefix, config);
            if (runtime.isPresent()) {
                return runtime;
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all runtimes matching a specific node type and domain.
     *
     * @param nodeType The workflow node type
     * @param domain   The component domain
     * @return List of matching runtimes
     */
    public List<ComponentRuntime> findByActorAndDomain(Actor actor,
            String domain) {
        String prefix = actor.name().toLowerCase() + ":" + domain.toLowerCase() + ":";
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
    public List<ComponentRuntime> findByActor(Actor actor) {
        String prefix = actor.name().toLowerCase() + ":";
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
     * @param config    The component configuration
     * @return true if the component was removed, false otherwise
     */
    public boolean unregister(WorkflowComponent component, ComponentRuntimeConfig config) {
        boolean anyRemoved = false;
        for (Actor actor : component.getActors()) {
            String key = buildRegistryKey(actor, component.getDomain(), config);
            if (unregister(key)) {
                anyRemoved = true;
            }
        }
        return anyRemoved;
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
        for (Actor actor : component.getActors()) {
            String prefix = buildPrefix(actor, component.getDomain());
            if (hasFactory(prefix)) {
                return true;
            }
        }
        return false;
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
        boolean anyRemoved = false;
        for (Actor actor : component.getActors()) {
            String prefix = buildPrefix(actor, component.getDomain());
            if (unregisterFactory(prefix)) {
                anyRemoved = true;
            }
        }
        return anyRemoved;
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
    private Optional<ComponentRuntime> createFromFactory(String prefix, ComponentRuntimeConfig config) {
        ComponentRuntimeFactory<?> factory = factoryRegistry.get(prefix.toLowerCase());
        if (factory != null) {
            try {
                ComponentRuntime runtime = factory.create();
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
    private String buildPrefix(Actor actor, String domain) {
        return String.format("%s:%s", actor.name().toLowerCase(), domain.toLowerCase());
    }

    /**
     * Builds the registry key in format: TYPE:domain:configHash
     * Example: task:com.example.sendEmail:1234123
     */
    private String buildRegistryKey(Actor actor, String domain, ComponentRuntimeConfig config) {
        int configHash = calculateConfigHash(config);

        return String.format("%s:%s:%d",
                actor.name().toLowerCase(),
                domain.toLowerCase(),
                configHash);
    }

    /**
     * Calculates hash code for the configuration.
     */
    private int calculateConfigHash(ComponentRuntimeConfig config) {
        if (config == null || config.getConfig() == null) {
            return 0;
        }
        return config.getConfig().hashCode();
    }
}
