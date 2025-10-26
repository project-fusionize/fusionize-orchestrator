package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.runtime.event.ComponentFinishedEventData;

public interface ComponentRuntimeTask extends ComponentRuntime {
    void run(ComponentEvent<ComponentFinishedEventData> onFinish);

}
