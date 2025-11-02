package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.WorkflowComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationInvocationRequestEventHandler implements EventHandler<InvocationRequestEvent> {
    private final WorkflowComponentRuntimeEngine runtimeEngine;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRegistry workflowExecutionRegistry;
    public OrchestrationInvocationRequestEventHandler(WorkflowComponentRuntimeEngine runtimeEngine,
                                                      WorkflowRegistry workflowRegistry,
                                                      WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.runtimeEngine = runtimeEngine;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }

    @Override
    public boolean shouldHandle(InvocationRequestEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(InvocationRequestEvent.class.getCanonicalName())
                && OrchestrationEvent.Origin.ORCHESTRATOR.equals(event.getOrigin());
    }

    @Override
    public Event handle(InvocationRequestEvent event) {
        event.ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        return runtimeEngine.invokeComponent(event);
    }

    @Override
    public Class<InvocationRequestEvent> getEventType() {
        return InvocationRequestEvent.class;
    }
}

