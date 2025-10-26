package dev.fusionize.workflow.component.event.data;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class ActivateEventData extends RuntimeEventData {
    private Boolean activated;

    public ActivateEventData() {
        super("ACTIVATE");
    }

    private ActivateEventData(Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
        setActivated(builder.activated);
    }

    public static ActivateEventData.Builder builder(){
        ActivateEventData ins = new ActivateEventData();
        return new ActivateEventData.Builder(ins.getEventTypeName());
    }

    public static class Builder extends RuntimeEventData.Builder<Builder, ActivateEventData> {
        private Boolean activated;

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        public Builder activate(Boolean activated) {
            this.activated = activated;
            return this;
        }

        @Override
        public ActivateEventData build() {
            return new ActivateEventData(this);
        }
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}
