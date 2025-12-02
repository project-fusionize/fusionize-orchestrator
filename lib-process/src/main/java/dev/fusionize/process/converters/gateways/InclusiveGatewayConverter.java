package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.SequenceFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.fusionize.process.ProcessConverter.buildKey;

public class InclusiveGatewayConverter extends ProcessNodeConverter<InclusiveGateway> {

    @Override
    public WorkflowNodeDescription convert(InclusiveGateway inclusiveGateway, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setComponentConfig(config);

        // Join: Multiple incoming flows
        if (inclusiveGateway.getIncomingFlows().size() > 1) {
            node.setType(WorkflowNodeType.WAIT);
            node.setComponent("join");
            List<String> await = new ArrayList<>();
            for (SequenceFlow flow : inclusiveGateway.getIncomingFlows()) {
                FlowElement sourceElement = model.getMainProcess().getFlowElement(flow.getSourceRef());
                if (sourceElement != null) {
                    await.add(buildKey(sourceElement));
                }
            }
            config.put("await", await);
            config.put("mergeStrategy", "pickLast");
            config.put("waitMode", "all"); // Defaulting to 'all' for inclusive join
            return node;
        }

        // Fork: Multiple outgoing flows
        if (inclusiveGateway.getOutgoingFlows().size() > 1) {
            node.setType(WorkflowNodeType.DECISION);
            node.setComponent("fork");

            if (inclusiveGateway.getDefaultFlow() != null) {
                FlowElement defaultFlow = model.getMainProcess().getFlowElement(inclusiveGateway.getDefaultFlow());
                if (defaultFlow instanceof SequenceFlow sequenceFlow) {
                    FlowElement targetElement = model.getMainProcess().getFlowElement(sequenceFlow.getTargetRef());
                    if (targetElement != null) {
                        config.put("default", buildKey(targetElement));
                    }
                }
            }

            Map<String, String> conditions = new HashMap<>();
            for (SequenceFlow flow : inclusiveGateway.getOutgoingFlows()) {
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

        // Fallback: No-op
        node.setType(WorkflowNodeType.TASK);
        node.setComponent("noop");
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof InclusiveGateway;
    }
}
