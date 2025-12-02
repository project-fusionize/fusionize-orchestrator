package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.ProcessNodeConverter;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;

import java.util.Map;

import static dev.fusionize.process.ProcessConverter.buildKey;

public class ParallelGatewayConverter extends ProcessNodeConverter<ParallelGateway> {

    @Override
    public WorkflowNodeDescription convert(ParallelGateway parallelGateway, BpmnModel model) {
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        Map<String, Object> config = new java.util.HashMap<>();
        node.setComponentConfig(config);

        if (parallelGateway.getIncomingFlows().size() <= 1) {
            node.setType(dev.fusionize.workflow.WorkflowNodeType.TASK);
            node.setComponent("noop");
            return node;
        }

        node.setType(dev.fusionize.workflow.WorkflowNodeType.WAIT);
        node.setComponent("join");
        java.util.List<String> await = new java.util.ArrayList<>();
        for (SequenceFlow flow : parallelGateway.getIncomingFlows()) {
            FlowElement sourceElement = model.getMainProcess().getFlowElement(flow.getSourceRef());
            if (sourceElement != null) {
                await.add(buildKey(sourceElement));
            }
        }
        config.put("await", await);
        config.put("mergeStrategy", "pickLast");
        config.put("waitMode", "all");

        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ParallelGateway;
    }
}
