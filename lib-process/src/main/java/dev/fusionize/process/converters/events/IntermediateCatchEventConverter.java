package dev.fusionize.process.converters.events;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.process.converters.events.definitions.MessageEventDefinitionConverter;
import dev.fusionize.process.converters.events.definitions.SignalEventDefinitionConverter;
import dev.fusionize.process.converters.events.definitions.TimerEventDefinitionConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;

import java.util.List;

public class IntermediateCatchEventConverter extends ProcessNodeConverter<IntermediateCatchEvent> {

    private final List<ProcessEventConverter<?>> converters = List.of(
            new TimerEventDefinitionConverter(),
            new MessageEventDefinitionConverter(WorkflowNodeType.WAIT),
            new SignalEventDefinitionConverter(WorkflowNodeType.WAIT));

    @Override
    public WorkflowNodeDescription convert(IntermediateCatchEvent intermediateCatchEvent, BpmnModel model) {
        WorkflowNodeDescription nodeDescription = getEventDefinitionConverter(model, intermediateCatchEvent, converters);
        if(nodeDescription != null) {
            return nodeDescription;
        }

        // Fallback if no supported event definition is found
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setType(WorkflowNodeType.WAIT);
        node.setComponent(NoopComponent.NAME);
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof IntermediateCatchEvent;
    }
}
