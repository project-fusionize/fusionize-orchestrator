package dev.fusionize.workflow.component.event.data;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class StartEventData extends RuntimeEventData {
    public StartEventData() {
        super("START");
    }
    private StartEventData(StartEventData.Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
    }

    public static StartEventData.Builder builder(){
        StartEventData ins = new StartEventData();
        return new StartEventData.Builder(ins.getEventTypeName());
    }

    public static class Builder extends RuntimeEventData.Builder<StartEventData.Builder, StartEventData> {

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        @Override
        public StartEventData build() {
            return new StartEventData(this);
        }
    }
}
