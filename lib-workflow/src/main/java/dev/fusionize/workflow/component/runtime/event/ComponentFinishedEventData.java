package dev.fusionize.workflow.component.runtime.event;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class ComponentFinishedEventData extends RuntimeEventData {
    private Boolean activated;
    public ComponentFinishedEventData() {
        super("COMPONENT_FINISHED");
    }
    private ComponentFinishedEventData(ComponentFinishedEventData.Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
    }

    public static ComponentFinishedEventData.Builder builder(){
        ComponentFinishedEventData ins = new ComponentFinishedEventData();
        return new ComponentFinishedEventData.Builder(ins.getEventTypeName());
    }

    @Override
    public void setOrigin(Origin origin) {
        super.setOrigin(Origin.COMPONENT);
    }

    public static class Builder extends RuntimeEventData.Builder<ComponentFinishedEventData.Builder, ComponentFinishedEventData> {
        private Boolean activated;

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        public Builder activate(boolean activated) {
            this.activated = activated;
            return this;
        }

        @Override
        public ComponentFinishedEventData build() {
            return new ComponentFinishedEventData(this);
        }
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }
}
