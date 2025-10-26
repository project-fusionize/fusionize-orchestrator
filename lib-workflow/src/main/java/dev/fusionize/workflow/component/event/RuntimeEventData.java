package dev.fusionize.workflow.component.event;

import dev.fusionize.workflow.WorkflowContext;

import java.util.HashMap;
import java.util.Map;

public abstract class RuntimeEventData {
    private final String eventTypeName;
    private Origin origin;
    private WorkflowContext context;
    private Exception exception;

    protected RuntimeEventData(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public enum Origin {
        COMPONENT("COMPONENT"),
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

    public static abstract class Builder<
            T extends Builder<T, D>,
            D extends RuntimeEventData
            > {

        protected String eventTypeName;
        public Origin origin;
        public WorkflowContext context;

        protected Builder(String eventTypeName) {
            this.eventTypeName = eventTypeName;
        }

        @SuppressWarnings("unchecked")
        public T origin(Origin origin) {
            this.origin = origin;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T context(WorkflowContext context) {
            this.context = context;
            return (T) this;
        }

        public abstract D build();
    }


    public String getEventTypeName() {
        return eventTypeName;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public WorkflowContext getContext() {
        return context;
    }

    public void setContext(WorkflowContext context) {
        this.context = context;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
