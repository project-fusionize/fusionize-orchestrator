package dev.fusionize.ai;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

public class TestEmitter implements ComponentUpdateEmitter {
    public boolean successCalled = false;
    public boolean failureCalled = false;
    public Exception lastFailure;
    public Context lastContext;

    @Override
    public void success(Context updatedContext) {
        successCalled = true;
        lastContext = updatedContext;
    }

    @Override
    public void failure(Exception ex) {
        failureCalled = true;
        lastFailure = ex;
    }

    @Override
    public Logger logger() {
        return (message, level, throwable) -> { };
    }

    @Override
    public InteractionLogger interactionLogger() {
        return (Object content,
                String actor,
                WorkflowInteraction.InteractionType type,
                WorkflowInteraction.Visibility visibility) -> { };
    }
}
