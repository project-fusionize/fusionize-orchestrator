package dev.fusionize.workflow.component.runtime.event;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class ComponentTriggeredEventData extends RuntimeEventData {
    private Boolean activated;
    public ComponentTriggeredEventData() {
        super("COMPONENT_TRIGGER");
    }
    private ComponentTriggeredEventData(ComponentTriggeredEventData.Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
    }

    public static ComponentTriggeredEventData.Builder builder(){
        ComponentTriggeredEventData ins = new ComponentTriggeredEventData();
        return new ComponentTriggeredEventData.Builder(ins.getEventTypeName());
    }

    @Override
    public void setOrigin(Origin origin) {
        super.setOrigin(Origin.COMPONENT);
    }

    public static class Builder extends RuntimeEventData.Builder<ComponentTriggeredEventData.Builder, ComponentTriggeredEventData> {
        private Boolean activated;

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        public Builder activate(boolean activated) {
            this.activated = activated;
            return this;
        }

        @Override
        public ComponentTriggeredEventData build() {
            return new ComponentTriggeredEventData(this);
        }
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}
