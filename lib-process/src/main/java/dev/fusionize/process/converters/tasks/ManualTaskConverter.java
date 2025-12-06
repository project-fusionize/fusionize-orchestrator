package dev.fusionize.process.converters.tasks;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ManualTask;

public class ManualTaskConverter extends ProcessNodeConverter<ManualTask> {
    @Override
    public WorkflowNodeDescription convert(ManualTask manualTask, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setType(WorkflowNodeType.TASK);
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ManualTask;
    }
}
