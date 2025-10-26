package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.event.ComponentTriggeredEventData;

public interface ComponentRuntimeWait extends ComponentRuntime {
    void wait(ComponentEvent<ComponentTriggeredEventData> onTriggered);
}
