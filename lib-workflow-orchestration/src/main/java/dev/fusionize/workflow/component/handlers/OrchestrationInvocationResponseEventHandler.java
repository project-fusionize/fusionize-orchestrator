package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.InvocationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationInvocationResponseEventHandler implements EventHandler<InvocationResponseEvent> {
    private final Orchestrator orchestrator;
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    public OrchestrationInvocationResponseEventHandler(Orchestrator orchestrator,
                                                       WorkflowRepoRegistry workflowRegistry,
                                                       WorkflowExecutionRepoRegistry workflowExecutionRegistry) {
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
