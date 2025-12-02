package dev.fusionize.process.converters.events;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.process.converters.events.definitions.MessageEventDefinitionConverter;
import dev.fusionize.process.converters.events.definitions.SignalEventDefinitionConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;

import java.util.List;

public class EndEventConverter extends ProcessNodeConverter<EndEvent> {

    private final List<ProcessEventConverter<?>> converters = List.of(
            new MessageEventDefinitionConverter(WorkflowNodeType.END),
            new SignalEventDefinitionConverter(WorkflowNodeType.END)
    );

    @Override
    public WorkflowNodeDescription convert(EndEvent endEvent, BpmnModel model) {
        WorkflowNodeDescription nodeDescription = getEventDefinitionConverter(model, endEvent, converters);
        if(nodeDescription != null) {
            return nodeDescription;
        }

        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setType(WorkflowNodeType.END);
        node.setComponent("end");
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof EndEvent;
    }
}
