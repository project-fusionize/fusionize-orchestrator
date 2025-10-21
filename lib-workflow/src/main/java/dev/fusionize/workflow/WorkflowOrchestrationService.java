package dev.fusionize.workflow;

import dev.fusionize.workflow.component.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WorkflowOrchestrationService {
    private final WorkflowComponentRegistry workflowComponentRegistry;

    public WorkflowOrchestrationService(WorkflowComponentRegistry workflowComponentRegistry) {
        this.workflowComponentRegistry = workflowComponentRegistry;
    }

    public void orchestrate(Workflow workflow){
        List<WorkflowNodeExecution> nodeExecutions = workflow.getNodes().stream()
                .map(n -> WorkflowNodeExecution.of(n,new WorkflowContext()))
                .peek(this::setWorkflowNodeExecutionRuntime)
                .peek(this::executeRuntime)
                .toList();
    }

    private void setWorkflowNodeExecutionRuntime(WorkflowNodeExecution ne){
        Optional<WorkflowComponentRuntime> optionalWorkflowComponentRuntime =  workflowComponentRegistry.get(
                ne.getWorkflowNode().getComponent(),
                ne.getWorkflowNode().getComponentConfig());
        if(optionalWorkflowComponentRuntime.isPresent()){
            ne.setRuntime(optionalWorkflowComponentRuntime.get());
        }else {
            optionalWorkflowComponentRuntime =  workflowComponentRegistry.get(
                    ne.getWorkflowNode().getComponent(), new WorkflowComponentConfig());
            if(optionalWorkflowComponentRuntime.isPresent()){
                try {
                    WorkflowComponentRuntime cloned = optionalWorkflowComponentRuntime.get().clone();
                    cloned.configure(ne.getWorkflowNode());
                    workflowComponentRegistry.register(ne.getWorkflowNode().getComponent(),
                            ne.getWorkflowNode().getComponentConfig(),cloned);
                    ne.setRuntime(cloned);
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    private void executeRuntime(WorkflowNodeExecution ne){
        if(ne.getRuntime() == null){
            return;
        }
        if(ne.getRuntime().canActivate(ne.getStageContext())){
            switch (ne.getWorkflowNode().getType()) {
                case START -> executeStart(ne);
                case DECISION -> executeDecision(ne);
                case TASK -> executeTask(ne);
                case WAIT -> executeWait(ne);
                case END -> executeEnd(ne);
            }
        }
    }

    private void executeStart(WorkflowNodeExecution ne){
        if(ne.getRuntime() instanceof WorkflowComponentRuntimeStart startRuntime){
            startRuntime.start((WorkflowContext start)->{
                System.out.println(start);
                return true;
            });
        }
    }

    private void executeDecision(WorkflowNodeExecution ne){
        if(ne.getRuntime() instanceof WorkflowComponentRuntimeDecision decisionRuntime){
            decisionRuntime.decide(ne.getStageContext(), (List<WorkflowNode> candidates)->{
                System.out.println(candidates);
                return true;
            });
        }
    }

    private void executeTask(WorkflowNodeExecution ne){
        if(ne.getRuntime() instanceof WorkflowComponentRuntimeTask taskRuntime){
            taskRuntime.run(ne.getStageContext(), (WorkflowContext finish)->{
                System.out.println(finish);
                return true;
            });
        }
    }

    private void executeWait(WorkflowNodeExecution ne){
        if(ne.getRuntime() instanceof WorkflowComponentRuntimeWait waitRuntime){
            waitRuntime.wait(ne.getStageContext(), (WorkflowContext resumed)->{
                System.out.println(resumed);
                return true;
            });
        }
    }

    private void executeEnd(WorkflowNodeExecution ne){
        if(ne.getRuntime() instanceof WorkflowComponentRuntimeEnd endRuntime){
            endRuntime.finish(ne.getStageContext(), (WorkflowExecutionStatus status)->{
                System.out.println(status);
                return true;
            });
        }
    }

}
