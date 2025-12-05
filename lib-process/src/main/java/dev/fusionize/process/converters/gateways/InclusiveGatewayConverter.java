package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.component.local.beans.ForkComponent;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.InclusiveGateway;

import java.util.Map;


public class InclusiveGatewayConverter extends GatewayConverter<InclusiveGateway> {

    @Override
    public WorkflowNodeDescription convert(InclusiveGateway inclusiveGateway, BpmnModel model) {
        if (isJoin(inclusiveGateway)) {
            WorkflowNodeDescription node = getJoinNode();
            Map<String, Object> config = node.getComponentConfig();
            config.put(JoinComponent.CONF_AWAIT, getIncomingFlows(inclusiveGateway, model));
            config.put(JoinComponent.CONF_MERGE_STRATEGY, JoinComponent.MergeStrategy.PICK_LAST.toString());
            config.put(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.ALL.toString());
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getComponentConfig();
        config.put(ForkComponent.CONF_FORK_MODE, ForkComponent.ForkMode.INCLUSIVE.toString());
        String defaultFlow = getDefaultFlow(inclusiveGateway, model);
        if (defaultFlow != null) {
            config.put(ForkComponent.CONF_DEFAULT_PATH, defaultFlow);
        }
        config.put(ForkComponent.CONF_CONDITIONS, getOutgoingFlows(inclusiveGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof InclusiveGateway;
    }
}
