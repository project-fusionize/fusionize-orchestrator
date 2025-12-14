package dev.fusionize.workflow.component.local;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalComponentRegistrar {
    private static final Logger log = LoggerFactory.getLogger(LocalComponentRegistrar.class);

    private final ComponentRuntimeRegistry componentRuntimeRegistry;
    private final List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories;

    public LocalComponentRegistrar(ComponentRuntimeRegistry componentRuntimeRegistry,
                                   List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> localComponentRuntimeFactories) {
        this.componentRuntimeRegistry = componentRuntimeRegistry;
        this.localComponentRuntimeFactories = localComponentRuntimeFactories;

        log.info("Registering {} local component factories", localComponentRuntimeFactories.size());
        for (LocalComponentRuntimeFactory<? extends LocalComponentRuntime> factory : localComponentRuntimeFactories) {
            String name = factory.getName();
            componentRuntimeRegistry.registerFactory(name, (ComponentRuntimeFactory<?>) factory);
            log.info("Registered local component factory: {}", name);
        }
    }
}
