package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.component.local.beans.ForkComponent;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ComplexGateway;
import org.flowable.bpmn.model.FlowElement;

import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexGatewayConverter extends GatewayConverter<ComplexGateway> {

    @Override
    public WorkflowNodeDescription convert(ComplexGateway complexGateway, BpmnModel model) {
        if (isJoin(complexGateway)) {
            WorkflowNodeDescription node = getJoinNode();
            Map<String, Object> config = node.getConfig();
            List<String> await = getIncomingFlows(complexGateway, model);
            config.put(JoinComponent.CONF_AWAIT, await);
            config.put(JoinComponent.CONF_MERGE_STRATEGY, JoinComponent.MergeStrategy.PICK_LAST.toString());
            config.put(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.THRESHOLD.toString());
            config.put(JoinComponent.CONF_THRESHOLD_CT, await.size() / 2);
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getConfig();
        String defaultFlow = getDefaultFlow(complexGateway, model);
        if (defaultFlow != null) {
            config.put(ForkComponent.CONF_DEFAULT_PATH, defaultFlow);
        }
        config.put(ForkComponent.CONF_CONDITIONS, getOutgoingFlows(complexGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ComplexGateway;
    }
}
