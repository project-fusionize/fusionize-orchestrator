package dev.fusionize.workflow.events;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.Map;

public abstract class OrchestrationEvent extends RuntimeEvent {
    private String workflowId;
    private String workflowExecutionId;
    private String workflowNodeId;
    private String workflowNodeExecutionId;
    private Origin origin;
    @Transient
    private OrchestrationEventContext orchestrationEventContext;

    protected OrchestrationEvent(Object source) {
        super(source);
    }

    public enum Origin {
        RUNTIME_ENGINE("RUNTIME_ENGINE"),
        ORCHESTRATOR("ORCHESTRATOR");

        private final String name;

        Origin(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        private static final Map<String, Origin> lookup = new HashMap<>();

        static{
            for(Origin origin : Origin.values())
                lookup.put(origin.getName(), origin);
        }
        public static Origin get(String name){
            return lookup.get(name);
        }
    }

    public abstract static class Builder<T extends Builder<T>> extends RuntimeEvent.Builder<T>{
        private String workflowId;
        private String workflowExecutionId;
        private String workflowNodeId;
        private String workflowNodeExecutionId;
        private Origin origin;
        private OrchestrationEventContext orchestrationEventContext;

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public T workflowId(String workflowId) {
            this.workflowId = workflowId;
            return self();
        }

        public T workflowExecutionId(String workflowExecutionId) {
            this.workflowExecutionId = workflowExecutionId;
            return self();
        }

        public T workflowNodeId(String workflowNodeId) {
            this.workflowNodeId = workflowNodeId;
            return self();
        }

        public T workflowNodeExecutionId(String workflowNodeExecutionId) {
            this.workflowNodeExecutionId = workflowNodeExecutionId;
            return self();
        }

        public T origin(Origin origin) {
            this.origin = origin;
            return self();
        }

        public T orchestrationEventContext(WorkflowExecution workflowExecution,
                                                 WorkflowNodeExecution nodeExecution) {
            this.orchestrationEventContext = new OrchestrationEventContext(workflowExecution, nodeExecution);
            return self();
        }

        public T orchestrationEventContext(OrchestrationEventContext orchestrationEventContext) {
            this.orchestrationEventContext = orchestrationEventContext;
            return self();
        }

        public void load(OrchestrationEvent event) {
            super.load(event);
            event.setOrigin(origin);
            event.setWorkflowId(workflowId);
            event.setWorkflowExecutionId(workflowExecutionId);
            event.setWorkflowNodeId(workflowNodeId);
            event.setWorkflowNodeExecutionId(workflowNodeExecutionId);
            event.setOrchestrationEventContext(orchestrationEventContext);
        }

    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(String workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

    public String getWorkflowNodeExecutionId() {
        return workflowNodeExecutionId;
    }

    public void setWorkflowNodeExecutionId(String workflowNodeExecutionId) {
        this.workflowNodeExecutionId = workflowNodeExecutionId;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public OrchestrationEventContext getOrchestrationEventContext() {
        return orchestrationEventContext;
    }

    public void setOrchestrationEventContext(OrchestrationEventContext orchestrationEventContext) {
        this.orchestrationEventContext = orchestrationEventContext;
    }
}
