package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.component.local.beans.ForkComponent;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;

import java.util.Map;

public class ExclusiveGatewayConverter extends GatewayConverter<ExclusiveGateway> {

    @Override
    public WorkflowNodeDescription convert(ExclusiveGateway exclusiveGateway, BpmnModel model) {
        if (isJoin(exclusiveGateway)) {
            WorkflowNodeDescription node = getJoinNode();
            Map<String, Object> config = node.getComponentConfig();
            config.put(JoinComponent.CONF_AWAIT, getIncomingFlows(exclusiveGateway, model));
            config.put(JoinComponent.CONF_MERGE_STRATEGY, JoinComponent.MergeStrategy.PICK_FIRST.toString());
            config.put(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.ANY);
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getComponentConfig();
        config.put(ForkComponent.CONF_FORK_MODE, ForkComponent.ForkMode.EXCLUSIVE);
        String defaultFlow = getDefaultFlow(exclusiveGateway, model);
        if (defaultFlow != null) {
            config.put(ForkComponent.CONF_DEFAULT_PATH, defaultFlow);
        }
        config.put(ForkComponent.CONF_CONDITIONS, getOutgoingFlows(exclusiveGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ExclusiveGateway;
    }
}
