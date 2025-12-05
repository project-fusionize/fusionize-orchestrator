package dev.fusionize.process.converters.gateways;

import dev.fusionize.process.converters.GatewayConverter;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ParallelGateway;

public class ParallelGatewayConverter extends GatewayConverter<ParallelGateway> {

    @Override
    public WorkflowNodeDescription convert(ParallelGateway parallelGateway, BpmnModel model) {
        if (isFork(parallelGateway)) {
            WorkflowNodeDescription node = new WorkflowNodeDescription();
            node.setType(WorkflowNodeType.TASK);
            node.setComponent(NoopComponent.NAME);
            return node;
        }

        WorkflowNodeDescription node = getJoinNode();
        node.getConfig().put(JoinComponent.CONF_AWAIT, getIncomingFlows(parallelGateway, model));
        node.getConfig().put(JoinComponent.CONF_MERGE_STRATEGY,
                JoinComponent.MergeStrategy.PICK_LAST.toString());
        node.getConfig().put(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.ALL.toString());
        return node;
    }

    @Override
    public boolean canConvert(FlowElement element) {
        return element instanceof ParallelGateway;
    }
}
