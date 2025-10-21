package dev.fusionize.workflow;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "workflow-execution")
public class WorkflowExecution {
    @Id
    private String id;
    private List<WorkflowNodeExecution> nodes;
    private WorkflowExecutionStatus status;
    private WorkflowContext context;
}
