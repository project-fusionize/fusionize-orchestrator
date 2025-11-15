package dev.fusionize.workflow;

import dev.fusionize.common.utility.KeyUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "workflow-execution")
public class WorkflowExecution {
    @Id
    private String id;
    private String workflowExecutionId;
    private String workflowId;
    private List<WorkflowNodeExecution> nodes = new ArrayList<>();
    private WorkflowExecutionStatus status;
    @Transient
    private Workflow workflow;

    public static WorkflowExecution of(Workflow workflow) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.workflow = workflow;
        execution.workflowId = workflow.getWorkflowId();
        execution.status = WorkflowExecutionStatus.IDLE;
        execution.workflowExecutionId = KeyUtil.getTimestampId("WEXE");
        return execution;
    }

    public WorkflowNodeExecution findNode(String workflowNodeExecutionId) {
        return nodes.stream()
                .filter(n -> n.getWorkflowNodeExecutionId().equals(workflowNodeExecutionId))
                .findFirst()
                .orElseGet(() -> nodes.stream()
                        .map(n -> n.findNode(workflowNodeExecutionId))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    public WorkflowExecution renew() {
        WorkflowExecution clone = new WorkflowExecution();
        clone.id = null;
        clone.workflowExecutionId = KeyUtil.getTimestampId("WEXE");
        clone.workflowId = this.workflowId;
        clone.status = WorkflowExecutionStatus.IDLE;
        clone.workflow = this.workflow;

        List<WorkflowNodeExecution> renewedNodes = new ArrayList<>();
        for (WorkflowNodeExecution node : this.nodes) {
            renewedNodes.add(node.renew());
        }
        clone.nodes = renewedNodes;

        return clone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<WorkflowNodeExecution> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowNodeExecution> nodes) {
        this.nodes = nodes;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecutionStatus status) {
        this.status = status;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }
}
