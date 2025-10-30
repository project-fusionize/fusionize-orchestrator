package dev.fusionize.workflow.events.orchestration;

import dev.fusionize.workflow.events.OrchestrationEvent;

public class StartResponseEvent extends OrchestrationEvent {
    public StartResponseEvent(Object source) {
        super(source);
    }

    public static Builder builder(Object source) {
        return new Builder(StartResponseEvent.class, source);
    }

    public static StartResponseEvent from(Object source,
                                             Origin origin,
                                          StartRequestEvent requestEvent) {
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
        public StartResponseEvent build() {
            StartResponseEvent event = new StartResponseEvent(source);
            super.load(event);
            return event;
        }
    }

}