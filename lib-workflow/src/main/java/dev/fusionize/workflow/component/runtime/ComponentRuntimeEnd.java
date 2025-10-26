package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.event.ComponentFinishedEventData;

public interface ComponentRuntimeEnd extends ComponentRuntime {
    void finish(ComponentEvent<ComponentFinishedEventData> onFinish);
}
