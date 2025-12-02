package dev.fusionize.process;

import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;

public abstract class ProcessEventConverter<T extends EventDefinition> {
    public abstract WorkflowNodeDescription convert(T definition, BpmnModel model);

    public abstract boolean canConvert(EventDefinition definition);
}
