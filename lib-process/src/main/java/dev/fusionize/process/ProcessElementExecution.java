package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.WorkflowContext;
import org.flowable.bpmn.model.FlowElement;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcessElementExecution {
    private String elementExecutionId;
    private String elementId;
    private String elementName;
    private String elementType;
    private ProcessElementExecutionState state;
    private WorkflowContext context;
    private List<ProcessElementExecution> children = new ArrayList<>();
    private String parentExecutionId;
    @Transient
    private FlowElement flowElement;

    public static ProcessElementExecution of(FlowElement flowElement, WorkflowContext context) {
        ProcessElementExecution execution = new ProcessElementExecution();
        execution.elementExecutionId = KeyUtil.getTimestampId("PELE");
        execution.elementId = flowElement.getId();
        execution.elementName = flowElement.getName();
        execution.elementType = flowElement.getClass().getSimpleName();
        execution.state = ProcessElementExecutionState.IDLE;
        execution.context = context != null ? context : new WorkflowContext();
        execution.flowElement = flowElement;
        return execution;
    }

    public ProcessElementExecution findElement(String elementExecutionId) {
        if (this.elementExecutionId.equals(elementExecutionId)) {
            return this;
        }
        return children.stream()
                .map(child -> child.findElement(elementExecutionId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public String getElementExecutionId() {
        return elementExecutionId;
    }

    public void setElementExecutionId(String elementExecutionId) {
        this.elementExecutionId = elementExecutionId;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public ProcessElementExecutionState getState() {
        return state;
    }

    public void setState(ProcessElementExecutionState state) {
        this.state = state;
    }

    public WorkflowContext getContext() {
        return context;
    }

    public void setContext(WorkflowContext context) {
        this.context = context;
    }

    public List<ProcessElementExecution> getChildren() {
        return children;
    }

    public void setChildren(List<ProcessElementExecution> children) {
        this.children = children;
    }

    public String getParentExecutionId() {
        return parentExecutionId;
    }

    public void setParentExecutionId(String parentExecutionId) {
        this.parentExecutionId = parentExecutionId;
    }

    public FlowElement getFlowElement() {
        return flowElement;
    }

    public void setFlowElement(FlowElement flowElement) {
        this.flowElement = flowElement;
    }
}

