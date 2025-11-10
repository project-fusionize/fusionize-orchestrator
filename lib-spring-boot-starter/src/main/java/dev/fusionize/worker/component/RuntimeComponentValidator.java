package dev.fusionize.worker.component;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.registry.WorkflowComponentRegistry;

public class RuntimeComponentValidator {
    private final WorkflowComponentRegistry componentRegistry;

    public RuntimeComponentValidator(WorkflowComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public boolean isValidComponentFactory(Class<?> componentFactoryClass){
        return ComponentRuntimeFactory.class.isAssignableFrom(componentFactoryClass);
    }

    public boolean isValidComponentDefinition(RuntimeComponentDefinition runtimeComponentDefinition){
        return !runtimeComponentDefinition.description().isEmpty();
    }
}
