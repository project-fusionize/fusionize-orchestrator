package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;

import java.util.HashMap;
import java.util.Map;

import static dev.fusionize.process.ProcessConverter.buildKey;

public class ExclusiveGatewayConverter extends ProcessNodeConverter<ExclusiveGateway> {

    @Override
    public WorkflowNodeDescription convert(ExclusiveGateway exclusiveGateway, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setComponentConfig(config);

        if(exclusiveGateway.getOutgoingFlows().size() <= 1){
            node.setType(WorkflowNodeType.TASK);
            node.setComponent("noop");
            return node;
        }

        node.setType(WorkflowNodeType.DECISION);
        node.setComponent("fork");

        if (exclusiveGateway.getDefaultFlow() != null) {
            FlowElement defaultFlow = model.getMainProcess().getFlowElement(exclusiveGateway.getDefaultFlow());
            if (defaultFlow instanceof SequenceFlow sequenceFlow) {
                FlowElement targetElement = model.getMainProcess().getFlowElement(sequenceFlow.getTargetRef());
                if (targetElement != null) {
                    config.put("default", buildKey(targetElement));
                }
            }
        }

        Map<String, String> conditions = new HashMap<>();
        for (SequenceFlow flow : exclusiveGateway.getOutgoingFlows()) {
            if (flow.getConditionExpression() != null) {
                FlowElement targetElement = model.getMainProcess().getFlowElement(flow.getTargetRef());
                if (targetElement != null) {
                    conditions.put(buildKey(targetElement), flow.getConditionExpression());
                }
            }
        }
        config.put("conditions", conditions);
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ExclusiveGateway;
    }
}
