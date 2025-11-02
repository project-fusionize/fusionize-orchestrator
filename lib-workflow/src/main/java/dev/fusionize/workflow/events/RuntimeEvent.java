package dev.fusionize.workflow.events;
import dev.fusionize.workflow.WorkflowContext;

public abstract class RuntimeEvent extends Event {
    private WorkflowContext context;
    private String component;
    private Exception exception;

    @Override
    public RuntimeEvent renew() {
        RuntimeEvent renewed = (RuntimeEvent) super.renew();
        renewed.setComponent(this.component);
        renewed.setContext(this.context);
        renewed.setException(this.exception);
        return renewed;
    }

    public abstract static class Builder<T extends Builder<T>> extends Event.Builder<T> {
        private WorkflowContext context;
        private String component;
        private Exception exception;

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public T context(WorkflowContext context) {
            this.context = context;
            return self();
        }

        public T component(String component) {
            this.component = component;
            return self();
        }

        public T exception(Exception exception) {
            this.exception = exception;
            return self();
        }

        public void load(RuntimeEvent event) {
            super.load(event);
            event.setContext(context);
            event.setComponent(component);
            event.setException(exception);
        }
    }

    public WorkflowContext getContext() {
        return context;
    }

    public void setContext(WorkflowContext context) {
        this.context = context;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}