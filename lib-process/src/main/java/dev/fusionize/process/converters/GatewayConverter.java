package dev.fusionize.process.converters;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.ForkComponent;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.fusionize.process.ProcessConverter.buildKey;

public abstract class GatewayConverter<G extends Gateway> extends ProcessNodeConverter<G> {
    protected boolean isFork(G gateway) {
        return gateway.getOutgoingFlows().size() > 1;
    }

    protected boolean isJoin(G gateway) {
        return gateway.getIncomingFlows().size() > 1;
    }

    protected String getDefaultFlow(G gateway, BpmnModel model) {
        if (gateway.getDefaultFlow() == null) {
            return null;
        }

        FlowElement defaultFlow = model.getMainProcess().getFlowElement(gateway.getDefaultFlow());
        if (defaultFlow instanceof SequenceFlow sequenceFlow) {
            FlowElement targetElement = model.getMainProcess().getFlowElement(sequenceFlow.getTargetRef());
            if (targetElement != null) {
                return buildKey(targetElement);
            }
        }

        return null;
    }

    protected List<String> getIncomingFlows(G gateway, BpmnModel model) {
        List<String> await = new ArrayList<>();
        for (SequenceFlow flow : gateway.getIncomingFlows()) {
            FlowElement sourceElement = model.getMainProcess().getFlowElement(flow.getSourceRef());
            if (sourceElement != null) {
                await.add(buildKey(sourceElement));
            }
        }
        return await;
    }

    protected Map<String, String> getOutgoingFlows(G gateway, BpmnModel model) {
        Map<String, String> conditions = new HashMap<>();
        for (SequenceFlow flow : gateway.getOutgoingFlows()) {
            if (flow.getConditionExpression() != null) {
                FlowElement targetElement = model.getMainProcess().getFlowElement(flow.getTargetRef());
                if (targetElement != null) {
                    conditions.put(buildKey(targetElement), flow.getConditionExpression());
                }
            }
        }
        return conditions;
    }

    protected WorkflowNodeDescription getForkNode() {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setConfig(config);
        node.setType(WorkflowNodeType.DECISION);
        node.setComponent(ForkComponent.NAME);
        return node;
    }

    protected WorkflowNodeDescription getJoinNode() {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new HashMap<>();
        node.setConfig(config);
        node.setType(WorkflowNodeType.WAIT);
        node.setComponent(JoinComponent.NAME);
        return node;
    }
}
