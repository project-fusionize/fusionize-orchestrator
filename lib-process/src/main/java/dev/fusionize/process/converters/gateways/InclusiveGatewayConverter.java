package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.WorkflowNodeType;
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
            config.put("await", getIncomingFlows(inclusiveGateway, model));
            config.put("mergeStrategy", "pickLast");
            config.put("waitMode", "all");
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getComponentConfig();
        config.put("forkMode", "inclusive");
        String defaultFlow = getDefaultFlow(inclusiveGateway, model);
        if (defaultFlow != null) {
            config.put("default", defaultFlow);
        }
        config.put("conditions", getOutgoingFlows(inclusiveGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof InclusiveGateway;
    }
}
