package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import dev.fusionize.workflow.events.orchestration.InvocationRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrchestratorComponentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorComponentDispatcher.class);
    private final EventPublisher<Event> eventPublisher;

    public OrchestratorComponentDispatcher(EventPublisher<Event> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void dispatchActivation(WorkflowExecution we, WorkflowNodeExecution ne) {
        if(we == null || ne == null || we.getWorkflow() == null || ne.getWorkflowNode() == null) {
            log.error("Invalid workflow execution, cannot dispatch activation");
            return;
        }
        String component = ne.getWorkflowNode().getComponent();
        if (component == null || component.trim().isEmpty()) {
            component = NoopComponent.NAME;
        }

        ActivationRequestEvent activationRequestEvent = ActivationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .workflowExecutionId(we.getWorkflowExecutionId())
                .workflowId(we.getWorkflowId())
                .workflowNodeId(ne.getWorkflowNodeId())
                .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                .orchestrationEventContext(we, ne)
                .component(component)
                .context(ne.getStageContext()).build();
        eventPublisher.publish(activationRequestEvent);
    }

    public void dispatchInvocation(WorkflowExecution we, WorkflowNodeExecution ne) {
        if(we == null || ne == null || we.getWorkflow() == null || ne.getWorkflowNode() == null) {
            log.error("Invalid workflow execution, cannot dispatch invocation");
            return;
        }
        String component = ne.getWorkflowNode().getComponent();
        if (component == null || component.trim().isEmpty()) {
            component = NoopComponent.NAME;
        }

        InvocationRequestEvent invocationRequestEvent = InvocationRequestEvent.builder(this)
                .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                .workflowExecutionId(we.getWorkflowExecutionId())
                .workflowId(we.getWorkflowId())
                .workflowNodeId(ne.getWorkflowNodeId())
                .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                .orchestrationEventContext(we, ne)
                .component(component)
                .context(ne.getStageContext())
                .build();
        eventPublisher.publish(invocationRequestEvent);
    }

}
