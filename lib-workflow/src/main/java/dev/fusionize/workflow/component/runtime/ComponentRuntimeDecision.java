package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.event.ComponentFinishedEventData;

public interface ComponentRuntimeDecision extends ComponentRuntime {
    void decide(ComponentEvent<ComponentFinishedEventData> onDecision);
}
