package dev.fusionize.process.converters.events;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.process.converters.events.definitions.MessageEventDefinitionConverter;
import dev.fusionize.process.converters.events.definitions.SignalEventDefinitionConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;

import java.util.List;

public class StartEventConverter extends ProcessNodeConverter<StartEvent> {

    private final List<ProcessEventConverter<?>> converters = List.of(
            new MessageEventDefinitionConverter(WorkflowNodeType.START),
            new SignalEventDefinitionConverter(WorkflowNodeType.START));

    @Override
    public WorkflowNodeDescription convert(StartEvent startEvent, BpmnModel model) {
        WorkflowNodeDescription nodeDescription = getEventDefinitionConverter(model, startEvent, converters);
        if(nodeDescription != null) {
            return nodeDescription;
        }

        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setType(WorkflowNodeType.START);
        node.setComponent("start");
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof StartEvent;
    }
}
