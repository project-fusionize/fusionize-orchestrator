package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ComplexGateway;
import org.flowable.bpmn.model.FlowElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexGatewayConverter extends GatewayConverter<ComplexGateway> {

    @Override
    public WorkflowNodeDescription convert(ComplexGateway complexGateway, BpmnModel model) {
        if (isJoin(complexGateway)) {
            WorkflowNodeDescription node = getJoinNode();
            Map<String, Object> config = node.getComponentConfig();
            List<String> await = getIncomingFlows(complexGateway, model);
            config.put("await", await);
            config.put("mergeStrategy", "pickLast");
            config.put("waitMode", "threshold");
            config.put("thresholdCount", await.size() / 2);
            return node;
        }

        WorkflowNodeDescription node = getForkNode();
        Map<String, Object> config = node.getComponentConfig();
        String defaultFlow = getDefaultFlow(complexGateway, model);
        if (defaultFlow != null) {
            config.put("default", defaultFlow);
        }
        config.put("conditions", getOutgoingFlows(complexGateway, model));
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ComplexGateway;
    }
}
