package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.process.ProcessEventConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.MessageEventDefinition;

import java.util.HashMap;
import java.util.Map;

public class MessageEventDefinitionConverter extends ProcessEventConverter<MessageEventDefinition> {

    private final WorkflowNodeType nodeType;

    public MessageEventDefinitionConverter(WorkflowNodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public WorkflowNodeDescription convert(MessageEventDefinition definition, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setConfig(config);
        node.setType(nodeType);
        node.setComponent("message");

        if (definition.getMessageRef() != null) {
            config.put("messageRef", definition.getMessageRef());
        }

        return node;
    }

    @Override
    public boolean canConvert(EventDefinition definition) {
        return definition instanceof MessageEventDefinition;
    }
}
