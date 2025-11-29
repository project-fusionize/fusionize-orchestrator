package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.springframework.stereotype.Component;

@Component
public class JoinComponentFactory implements LocalComponentRuntimeFactory<JoinComponent> {
    private final WorkflowExecutionRegistry workflowExecutionRegistry;

    public JoinComponentFactory(WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }


    @Override
    public String getName() {
        return "join";
    }

    @Override
    public JoinComponent create() {
        return new JoinComponent(this.workflowExecutionRegistry);
    }
}
