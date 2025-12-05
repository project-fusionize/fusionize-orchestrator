package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;

import java.util.HashMap;
import java.util.Map;

public class TimerEventDefinitionConverter extends ProcessEventConverter<TimerEventDefinition> {
    @Override
    public WorkflowNodeDescription convert(TimerEventDefinition timerEventDefinition, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setConfig(config);
        node.setType(WorkflowNodeType.WAIT);
        node.setComponent(DelayComponent.NAME);
        if (timerEventDefinition.getTimeDuration() != null) {
            try {
                long delay = java.time.Duration.parse(timerEventDefinition.getTimeDuration()).toMillis();
                config.put(DelayComponent.CONF_DELAY, delay);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid duration format for timer event: " + timerEventDefinition.getTimeDuration(), e);
            }
        }
        return node;
    }

    @Override
    public boolean canConvert(EventDefinition definition) {
        return definition instanceof TimerEventDefinition;
    }
}
