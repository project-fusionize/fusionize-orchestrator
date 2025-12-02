package dev.fusionize.process;

import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;

import java.util.List;

public abstract class ProcessNodeConverter<T extends FlowElement> {
    public abstract WorkflowNodeDescription convert(T t, BpmnModel model);
    public abstract boolean canConvert(FlowElement element);

    protected WorkflowNodeDescription  getEventDefinitionConverter(
            BpmnModel model, Event event, List<ProcessEventConverter<?>> converters) {
        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            for (ProcessEventConverter<?> converter : converters) {
                if (converter.canConvert(eventDefinition)) {
                    @SuppressWarnings("unchecked")
                    ProcessEventConverter<EventDefinition> typedConverter =
                            (ProcessEventConverter<EventDefinition>) converter;
                    return typedConverter.convert(eventDefinition, model);
                }
            }
        }
        return null;
    }

}
