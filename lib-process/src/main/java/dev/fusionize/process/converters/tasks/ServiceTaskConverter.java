package dev.fusionize.process.converters.tasks;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;

import java.util.HashMap;
import java.util.Map;

public class ServiceTaskConverter extends ProcessNodeConverter<ServiceTask> {
    @Override
    public WorkflowNodeDescription convert(ServiceTask serviceTask, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setComponentConfig(config);
        node.setType(WorkflowNodeType.TASK);
        node.setComponent(serviceTask.getImplementation());
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ServiceTask;
    }
}
