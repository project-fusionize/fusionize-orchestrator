package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.event.ComponentTriggeredEventData;

public interface ComponentRuntimeStart extends ComponentRuntime {
    void start(ComponentEvent<ComponentTriggeredEventData> onTriggered);
}
