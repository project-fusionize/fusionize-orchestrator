package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationInvocationResponseEventHandler implements EventHandler<InvocationResponseEvent> {
    private final Orchestrator orchestrator;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRegistry workflowExecutionRegistry;
    public OrchestrationInvocationResponseEventHandler(Orchestrator orchestrator,
                                                       WorkflowRegistry workflowRegistry,
                                                       WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.orchestrator = orchestrator;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }


    @Override
    public boolean shouldHandle(InvocationResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(InvocationResponseEvent.class.getCanonicalName())
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(InvocationResponseEvent event) {
        event.ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        orchestrator.onInvoked(event);
        return null;
    }

    @Override
    public Class<InvocationResponseEvent> getEventType() {
        return InvocationResponseEvent.class;
    }
}
