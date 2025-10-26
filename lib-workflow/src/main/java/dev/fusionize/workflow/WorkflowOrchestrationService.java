package dev.fusionize.workflow;

import dev.fusionize.workflow.component.event.RuntimeEvent;
import dev.fusionize.workflow.component.event.RuntimeEventData;
import dev.fusionize.workflow.component.event.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowOrchestrationService {
    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrationService.class);
    private final ApplicationEventPublisher eventPublisher;

    public WorkflowOrchestrationService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void orchestrate(Workflow workflow) {
        WorkflowExecution we = WorkflowExecution.of(workflow);
        List<WorkflowNodeExecution> nodeExecutions = workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n, new WorkflowContext()))
                .peek(ne -> requestActivate(we, ne))
                .toList();
    }

    private void orchestrate(WorkflowNode workflowNode) {

    }

    private void requestActivate(WorkflowExecution we, WorkflowNodeExecution ne) {
        RuntimeEvent<ActivateEventData> activateRequestEvent =
                new RuntimeEvent.Builder<ActivateEventData>(this)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .component(ne.getWorkflowNode().getComponent())
                        .variant(RuntimeEvent.Variant.REQUEST)
                        .data(ActivateEventData.builder()
                                .context(ne.getStageContext())
                                .origin(RuntimeEventData.Origin.ORCHESTRATOR)
                                .build()
                        ).build();
        activateRequestEvent.ne = ne;
        activateRequestEvent.we = we;
        eventPublisher.publishEvent(activateRequestEvent);
    }

    private void requestStart(WorkflowExecution we, WorkflowNodeExecution ne) {
        RuntimeEvent<StartEventData> startRequestEvent =
                new RuntimeEvent.Builder<StartEventData>(this)
                        .workflowExecutionId(we.getWorkflowExecutionId())
                        .workflowId(we.getWorkflowId())
                        .workflowNodeId(ne.getWorkflowNodeId())
                        .workflowNodeExecutionId(ne.getWorkflowNodeExecutionId())
                        .component(ne.getWorkflowNode().getComponent())
                        .variant(RuntimeEvent.Variant.REQUEST)
                        .data(StartEventData.builder()
                                .context(ne.getStageContext())
                                .origin(RuntimeEventData.Origin.ORCHESTRATOR)
                                .build()
                        ).build();
        startRequestEvent.ne = ne;
        startRequestEvent.we = we;
        eventPublisher.publishEvent(startRequestEvent);
    }

    private void requestStartDecision(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    private void requestStartTask(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    private void requestStartWait(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    private void requestEnd(WorkflowExecution we, WorkflowNodeExecution ne) {

    }

    @EventListener
    public void handleActivateEvent(RuntimeEvent<ActivateEventData> event) {
        if(!shouldHandle(event, new ActivateEventData().getEventTypeName())){
            return;
        }
        if(RuntimeEvent.Variant.RESPONSE.equals(event.getVariant())){
            if(event.getData().getException()!=null){
                //todo handle exception
                log.error(event.getData().getException().getMessage(), event.getData().getException());

            }else if (event.getData().getActivated()){
                switch (event.ne.getWorkflowNode().getType()) {
                    case START -> requestStart(event.we, event.ne);
                    case DECISION -> requestStartDecision(event.we, event.ne);
                    case TASK -> requestStartTask(event.we, event.ne);
                    case WAIT -> requestStartWait(event.we, event.ne);
                    case END -> requestEnd(event.we, event.ne);
                }
            }
        }

    }

    @EventListener
    public void handleEndEvent(RuntimeEvent<EndEventData> event) {

    }


    @EventListener
    public void handleStartEvent(RuntimeEvent<StartEventData> event) {
        if(!shouldHandle(event, new StartEventData().getEventTypeName())){
            return;
        }
        if(RuntimeEvent.Variant.RESPONSE.equals(event.getVariant())){
            if(event.getData().getException()!=null){
                //todo handle exception
                log.error(event.getData().getException().getMessage(), event.getData().getException());

            }else {
                log.info(event.getData().getContext().toString());
            }
        }
    }


    private boolean shouldHandle(RuntimeEvent re, String e){
        return re!=null && !re.isProcessed() && !this.equals(re.getSource())
                && re.getData().getEventTypeName().equals(e)
                && RuntimeEventData.Origin.RUNTIME_ENGINE.equals(re.getData().getOrigin());
    }

}
