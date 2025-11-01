package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class InvocationResponseEvent extends OrchestrationEvent {
    public InvocationResponseEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(InvocationResponseEvent.class, source);
    }

    public static InvocationResponseEvent from(Object source,
                                               Origin origin,
                                               InvocationRequestEvent requestEvent) {
        return builder(source).workflowId(requestEvent.getWorkflowId())
                .causationId(requestEvent.getEventId())
                .correlationId(requestEvent.getCorrelationId())
                .workflowExecutionId(requestEvent.getWorkflowExecutionId())
                .workflowNodeId(requestEvent.getWorkflowNodeId())
                .workflowNodeExecutionId(requestEvent.getWorkflowNodeExecutionId())
                .orchestrationEventContext(requestEvent.getOrchestrationEventContext())
                .context(requestEvent.getContext())
                .component(requestEvent.getComponent())
                .origin(origin)
                .build();

    }

    public static class Builder extends OrchestrationEvent.Builder<Builder> {

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public InvocationResponseEvent build() {
            InvocationResponseEvent event = new InvocationResponseEvent(source);
            super.load(event);
            return event;
        }
    }

}