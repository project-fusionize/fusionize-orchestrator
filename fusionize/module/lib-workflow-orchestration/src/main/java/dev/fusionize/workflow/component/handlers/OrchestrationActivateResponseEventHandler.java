package dev.fusionize.workflow.component.handlers;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventHandler;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationResponseEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrchestrationActivateResponseEventHandler implements EventHandler<ActivationResponseEvent> {
    private final Orchestrator orchestrator;
    private final WorkflowRepoRegistry workflowRegistry;
    private final WorkflowExecutionRepoRegistry workflowExecutionRegistry;
    public OrchestrationActivateResponseEventHandler(Orchestrator orchestrator,
                                                     WorkflowRepoRegistry workflowRegistry,
                                                     WorkflowExecutionRepoRegistry workflowExecutionRegistry) {
        this.orchestrator = orchestrator;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }


    @Override
    public boolean shouldHandle(ActivationResponseEvent event) {
        return event!=null && event.getProcessedDate()==null
                && event.getEventClass().equals(ActivationResponseEvent.class.getCanonicalName())
                && OrchestrationEvent.Origin.RUNTIME_ENGINE.equals(event.getOrigin());
    }

    @Override
    public Event handle(ActivationResponseEvent event) throws Exception {
        event.ensureOrchestrationEventContext(workflowExecutionRegistry, workflowRegistry);
        orchestrator.onActivated(event);
        return null;
    }

    @Override
    public Class<ActivationResponseEvent> getEventType() {
        return ActivationResponseEvent.class;
    }
}
