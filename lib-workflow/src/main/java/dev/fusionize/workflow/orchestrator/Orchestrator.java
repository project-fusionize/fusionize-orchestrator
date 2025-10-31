package dev.fusionize.workflow.orchestrator;

import dev.fusionize.workflow.*;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.events.OrchestrationEventContext;
import dev.fusionize.workflow.events.orchestration.ActivateRequestEvent;
import dev.fusionize.workflow.events.orchestration.ActivateResponseEvent;
import dev.fusionize.workflow.events.orchestration.StartRequestEvent;
import dev.fusionize.workflow.events.orchestration.StartResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Orchestrator {
    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private final EventPublisher<Event> eventPublisher;

    public Orchestrator(EventPublisher<Event> publisher) {
        this.eventPublisher = publisher;
    }

    public void orchestrate(Workflow workflow) {
        WorkflowExecution we = WorkflowExecution.of(workflow);
         workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.empty()))
                .forEach(ne -> requestActivate(we, ne));
    }

    private void proceed(WorkflowExecution we, WorkflowNodeExecution ne) {
        List<WorkflowNodeExecution> nodeExecutions = ne.getWorkflowNode().getChildren().stream()
                        .map(n -> WorkflowNodeExecution.of(n, WorkflowContextFactory.from(ne, n)))
                        .peek(cne -> requestActivate(we, cne)).toList();
        ne.getChildren().addAll(nodeExecutions);
        we.getNodes().add(ne);
    }

    private void requestActivate(WorkflowExecution we, WorkflowNodeExecution ne) {
        ActivateRequestEvent activateRequestEvent =
                ActivateRequestEvent.builder(this)
                        .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .orchestrationEventContext(we, ne)
                        .component(ne.getWorkflowNode().getComponent())
                        .context(ne.getStageContext()).build();
        eventPublisher.publish(activateRequestEvent);
    }

    public void onActivated(ActivateResponseEvent activateResponseEvent){
        if(activateResponseEvent.getException()!=null){
            //todo handle exception
            log.error("Error -> {}", activateResponseEvent.getException().getMessage(), activateResponseEvent.getException());

        }else {
            OrchestrationEventContext oc = activateResponseEvent.getOrchestrationEventContext();
            switch (oc.getNodeExecution().getWorkflowNode().getType()) {
                case START -> requestStart(oc.getWorkflowExecution(), oc.getNodeExecution());
                case DECISION -> requestStartDecision(oc.getWorkflowExecution(), oc.getNodeExecution());
                case TASK -> requestStartTask(oc.getWorkflowExecution(), oc.getNodeExecution());
                case WAIT -> requestStartWait(oc.getWorkflowExecution(), oc.getNodeExecution());
                case END -> requestEnd(oc.getWorkflowExecution(), oc.getNodeExecution());
            }
        }
    }


    private void requestStart(WorkflowExecution we, WorkflowNodeExecution ne) {
        StartRequestEvent startRequestEvent =
                StartRequestEvent.builder(this)
                        .origin(OrchestrationEvent.Origin.ORCHESTRATOR)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .orchestrationEventContext(we, ne)
                        .component(ne.getWorkflowNode().getComponent())
                        .context(ne.getStageContext())
                       .build();
        eventPublisher.publish(startRequestEvent);
    }

    public void onStarted(StartResponseEvent startResponseEvent){
        if(startResponseEvent.getException()!=null){
            //todo handle exception
            log.error(startResponseEvent.getException().getMessage(), startResponseEvent.getException());
            return;
        }
        log.info(startResponseEvent.getContext().toString());
        OrchestrationEventContext oc = startResponseEvent.getOrchestrationEventContext();
        oc.getNodeExecution().setStageContext(startResponseEvent.getContext());
        proceed(oc.getWorkflowExecution(), oc.getNodeExecution());
    }

    private void requestStartDecision(WorkflowExecution we, WorkflowNodeExecution ne) {
        log.info("Not implemented yet");

    }

    private void requestStartTask(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    private void requestStartWait(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    private void requestEnd(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

}
