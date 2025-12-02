package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;

import java.util.HashMap;
import java.util.Map;

public class SignalEventDefinitionConverter extends ProcessEventConverter<SignalEventDefinition> {

    private final WorkflowNodeType nodeType;

    public SignalEventDefinitionConverter(WorkflowNodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public WorkflowNodeDescription convert(SignalEventDefinition definition, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setComponentConfig(config);
        node.setType(nodeType);
        node.setComponent("signal");

        if (definition.getSignalRef() != null) {
            config.put("signalRef", definition.getSignalRef());
        }

        return node;
    }

    @Override
    public boolean canConvert(EventDefinition definition) {
        return definition instanceof SignalEventDefinition;
    }
}
