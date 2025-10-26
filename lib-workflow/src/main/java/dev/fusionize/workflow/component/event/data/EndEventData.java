package dev.fusionize.workflow.component.event.data;

import dev.fusionize.workflow.component.event.RuntimeEventData;

public class EndEventData extends RuntimeEventData {
    public EndEventData() {
        super("END");
    }
    private EndEventData(EndEventData.Builder builder) {
        this();
        setOrigin(builder.origin);
        setContext(builder.context);
    }

    public static EndEventData.Builder builder(){
        EndEventData ins = new EndEventData();
        return new EndEventData.Builder(ins.getEventTypeName());
    }

    public static class Builder extends RuntimeEventData.Builder<EndEventData.Builder, EndEventData> {

        private Builder(String eventTypeName) {
            super(eventTypeName);
        }

        @Override
        public EndEventData build() {
            return new EndEventData(this);
        }
    }
}