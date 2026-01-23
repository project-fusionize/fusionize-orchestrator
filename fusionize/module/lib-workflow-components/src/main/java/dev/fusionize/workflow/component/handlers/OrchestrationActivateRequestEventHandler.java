package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationActivateRequestEventHandler implements EventHandler<ActivationRequestEvent> {
    private final ComponentRuntimeEngine componentRuntimeEngine;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRegistry workflowExecutionRegistry;

    public OrchestrationActivateRequestEventHandler(ComponentRuntimeEngine componentRuntimeEngine,
                                                    WorkflowRegistry workflowRegistry,
                                                    WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.componentRuntimeEngine = componentRuntimeEngine;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }

    @Override
    public boolean shouldHandle(ActivationRequestEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ActivationRequestEvent.class.getCanonicalName())
                && OrchestrationEvent.Origin.ORCHESTRATOR.equals(event.getOrigin());
    }

    @Override
    public Event handle(ActivationRequestEvent event) throws Exception {
        event.ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        return componentRuntimeEngine.activateComponent(event);
    }

    @Override
    public Class<ActivationRequestEvent> getEventType() {
        return ActivationRequestEvent.class;
    }
}
