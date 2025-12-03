package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
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
            config.put("await", getIncomingFlows(exclusiveGateway, model));
            config.put("mergeStrategy", "pickFirst");
            config.put("waitMode", "any");
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getComponentConfig();
        config.put("forkMode", "exclusive");
        String defaultFlow = getDefaultFlow(exclusiveGateway, model);
        if (defaultFlow != null) {
            config.put("default", defaultFlow);
        }
        config.put("conditions", getOutgoingFlows(exclusiveGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ExclusiveGateway;
    }
}
