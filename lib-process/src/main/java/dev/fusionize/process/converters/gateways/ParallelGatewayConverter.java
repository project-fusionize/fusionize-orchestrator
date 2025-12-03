package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;

public class ParallelGatewayConverter extends GatewayConverter<ParallelGateway> {

    @Override
    public WorkflowNodeDescription convert(ParallelGateway parallelGateway, BpmnModel model) {
        if (isFork(parallelGateway)) {
            WorkflowNodeDescription node = new WorkflowNodeDescription();
            node.setType(dev.fusionize.workflow.WorkflowNodeType.TASK);
            node.setComponent("noop");
            return node;
        }

        WorkflowNodeDescription node = getJoinNode();
        node.getComponentConfig().put("await", getIncomingFlows(parallelGateway, model));
        node.getComponentConfig().put("mergeStrategy", "pickLast");
        node.getComponentConfig().put("waitMode", "all");
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ParallelGateway;
    }
}
