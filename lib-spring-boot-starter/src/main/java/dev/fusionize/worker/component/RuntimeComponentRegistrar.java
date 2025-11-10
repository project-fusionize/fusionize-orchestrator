package dev.fusionize.worker.component;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.registry.WorkflowComponentRegistry;

public class RuntimeComponentRegistrar {
    private final WorkflowComponentRegistry componentRegistry;

    public RuntimeComponentRegistrar(WorkflowComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public boolean isValidComponentFactory(Class<?> componentFactoryClass){
        return ComponentRuntimeFactory.class.isAssignableFrom(componentFactoryClass);
    }

    public boolean isValidComponentDefinition(RuntimeComponentDefinition runtimeComponentDefinition){
        return runtimeComponentDefinition.type()!=null &&
                !runtimeComponentDefinition.name().isEmpty() &&
                !runtimeComponentDefinition.description().isEmpty();
    }

    public WorkflowComponent registerComponent(RuntimeComponentDefinition runtimeComponentDefinition){
        String domain = runtimeComponentDefinition.type().getCanonicalName();
        WorkflowComponent newWorkflowComponent = WorkflowComponent.builder("")
                .withDescription(runtimeComponentDefinition.description())
                .withCompatible(runtimeComponentDefinition.compatible())
                .withName(runtimeComponentDefinition.name())
                .withDomain(domain)
                .build();
        WorkflowComponent workflowComponent = componentRegistry.getWorkflowComponentByDomain(newWorkflowComponent.getDomain());
        if(workflowComponent==null){
            return componentRegistry.register(newWorkflowComponent);
        }else {
            workflowComponent.setName(runtimeComponentDefinition.name());
            workflowComponent.setDescription(runtimeComponentDefinition.description());
            workflowComponent.setCompatible(runtimeComponentDefinition.compatible());
            return componentRegistry.register(workflowComponent);
        }
    }
}
