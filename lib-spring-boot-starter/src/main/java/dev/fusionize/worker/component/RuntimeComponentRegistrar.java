package dev.fusionize.worker.component;

import java.util.Set;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.registery.WorkflowComponentRegistry;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;

public class RuntimeComponentRegistrar {
    private final WorkflowComponentRegistry componentRegistry;

    public RuntimeComponentRegistrar(WorkflowComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public boolean isValidComponentFactory(Class<?> componentFactoryClass) {
        return ComponentRuntimeFactory.class.isAssignableFrom(componentFactoryClass);
    }

    public boolean isValidComponentDefinition(RuntimeComponentDefinition runtimeComponentDefinition) {
        return runtimeComponentDefinition.type() != null &&
                !runtimeComponentDefinition.name().isEmpty() &&
                !runtimeComponentDefinition.description().isEmpty();
    }

    public WorkflowComponent registerComponent(RuntimeComponentDefinition runtimeComponentDefinition) {
        String domain = runtimeComponentDefinition.type().getCanonicalName();
        WorkflowComponent newWorkflowComponent = WorkflowComponent.builder("")
                .withDescription(runtimeComponentDefinition.description())
                .withActors(Set.of(runtimeComponentDefinition.actors()))
                .withName(runtimeComponentDefinition.name())
                .withDomain(domain)
                .build();
        return componentRegistry.register(newWorkflowComponent);
    }
}
