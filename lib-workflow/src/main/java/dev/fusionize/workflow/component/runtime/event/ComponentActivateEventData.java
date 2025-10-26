package dev.fusionize.workflow.component.runtime.event;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class ComponentActivateEventData extends RuntimeEventData {
    private Boolean activated;
    public ComponentActivateEventData() {
        super("COMPONENT_ACTIVATE");
    }
    private ComponentActivateEventData(ComponentActivateEventData.Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
    }

    public static ComponentActivateEventData.Builder builder(){
        ComponentActivateEventData ins = new ComponentActivateEventData();
        return new ComponentActivateEventData.Builder(ins.getEventTypeName());
    }

    @Override
    public void setOrigin(Origin origin) {
        super.setOrigin(Origin.COMPONENT);
    }

    public static class Builder extends RuntimeEventData.Builder<ComponentActivateEventData.Builder, ComponentActivateEventData> {
        private Boolean activated;

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        public Builder activate(boolean activated) {
            this.activated = activated;
            return this;
        }

        @Override
        public ComponentActivateEventData build() {
            return new ComponentActivateEventData(this);
        }
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}
