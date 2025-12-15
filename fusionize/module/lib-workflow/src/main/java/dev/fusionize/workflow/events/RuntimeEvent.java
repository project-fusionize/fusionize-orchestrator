package dev.fusionize.workflow.events;
import dev.fusionize.workflow.context.Context;

public abstract class RuntimeEvent extends Event {
    private Context context;
    private String component;
    private SerializableError exception;

    public static class SerializableError {
        private String className;
        private String message;

        public SerializableError() {}

        public SerializableError(String className, String message) {
            this.className = className;
            this.message = message;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Override
    public RuntimeEvent renew() {
        RuntimeEvent renewed = (RuntimeEvent) super.renew();
        renewed.setComponent(this.component);
        renewed.setContext(this.context);
        renewed.exception = this.exception;
        return renewed;
    }

    public abstract static class Builder<T extends Builder<T>> extends Event.Builder<T> {
        private Context context;
        private String component;
        private Throwable exception;

        protected Builder(Class<?> eventClass, Object source) {
            super(eventClass, source);
        }

        public T context(Context context) {
            this.context = context;
            return self();
        }

        public T component(String component) {
            this.component = component;
            return self();
        }

        public T exception(Throwable exception) {
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

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Throwable getException() {
        if (exception == null) {
            return null;
        }
        return new RuntimeException(exception.getClassName() + ": " + exception.getMessage());
    }

    public void setException(Throwable exception) {
        if (exception != null) {
            this.exception = new SerializableError(exception.getClass().getName(), exception.getMessage());
        } else {
            this.exception = null;
        }
    }
}