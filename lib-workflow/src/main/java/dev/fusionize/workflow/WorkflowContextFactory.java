package dev.fusionize.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowContextFactory {
    public static WorkflowContext empty() {
        return new WorkflowContext();
    }

    public static WorkflowContext from(WorkflowNodeExecution lastExecution, WorkflowNode nextNode) {
        WorkflowContext context = new WorkflowContext();
        if(lastExecution.getStageContext()!=null){
            context = lastExecution.getStageContext().renew();
        }
        if(nextNode!=null){
            switch(nextNode.getType()){
                case DECISION:
                    WorkflowDecision workflowDecision = new WorkflowDecision();
                    workflowDecision.setDecisionNode(nextNode.getWorkflowNodeKey());
                    Map<String,Boolean> options = new HashMap<>();
                    nextNode.getChildren().forEach(cn -> options.put(cn.getWorkflowNodeKey(),false));
                    workflowDecision.setOptionNodes(options);
                    context.getDecisions().add(workflowDecision);
                    break;
                case START:
                case TASK:
                case WAIT:
                case END:
                    break;
            }

        }
        return context;
    }
}
