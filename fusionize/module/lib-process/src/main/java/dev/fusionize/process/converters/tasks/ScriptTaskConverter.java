package dev.fusionize.process.converters.tasks;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.ScriptComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ScriptTask;

import java.util.HashMap;
import java.util.Map;

public class ScriptTaskConverter extends ProcessNodeConverter<ScriptTask> {
    @Override
    public WorkflowNodeDescription convert(ScriptTask scriptTask, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setConfig(config);
        node.setType(WorkflowNodeType.TASK);
        node.setComponent(ScriptComponent.NAME);
        config.put(ScriptComponent.CONF_PARSER, scriptTask.getScriptFormat());
        config.put(ScriptComponent.CONF_SCRIPT, scriptTask.getScript());
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ScriptTask;
    }
}
