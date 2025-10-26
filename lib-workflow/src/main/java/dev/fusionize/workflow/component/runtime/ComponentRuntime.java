package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.WorkflowComponentConfig;
import dev.fusionize.workflow.component.runtime.event.ComponentActivateEventData;

public interface ComponentRuntime {
    void configure(WorkflowComponentConfig config);
    void canActivate(ComponentEvent<ComponentActivateEventData> onActivate);
}
