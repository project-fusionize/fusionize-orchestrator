package dev.fusionize.process.converters.gateways;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.ForkComponent;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusiveGatewayConverterTest {

    private final ExclusiveGatewayConverter converter = new ExclusiveGatewayConverter();

    @Test
    void shouldConvertJoinGateway() {
        // setup
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("test-process");
        model.addProcess(process);

        ExclusiveGateway gateway = new ExclusiveGateway();
        gateway.setId("gw1");
        process.addFlowElement(gateway);

        ManualTask task1 = new ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        ManualTask task2 = new ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("task1");
        flow1.setTargetRef("gw1");
        gateway.getIncomingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("task2");
        flow2.setTargetRef("gw1");
        gateway.getIncomingFlows().add(flow2);

        // expectation
        WorkflowNodeDescription result = converter.convert(gateway, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.WAIT);
        assertThat(result.getComponent()).isEqualTo(JoinComponent.NAME);
        assertThat(result.getConfig()).containsEntry(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.ANY);
        assertThat(result.getConfig()).containsEntry(JoinComponent.CONF_MERGE_STRATEGY, JoinComponent.MergeStrategy.PICK_FIRST.toString());
        assertThat((List<String>) result.getConfig().get(JoinComponent.CONF_AWAIT)).hasSize(2);
    }

    @Test
    void shouldConvertForkGateway() {
        // setup
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("test-process");
        model.addProcess(process);

        ExclusiveGateway gateway = new ExclusiveGateway();
        gateway.setId("gw1");
        process.addFlowElement(gateway);

        ManualTask task1 = new ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        ManualTask task2 = new ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("gw1");
        flow1.setTargetRef("task1");
        flow1.setConditionExpression("${x > 10}");
        gateway.getOutgoingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("gw1");
        flow2.setTargetRef("task2");
        flow2.setConditionExpression("${x <= 10}");
        gateway.getOutgoingFlows().add(flow2);

        // expectation
        WorkflowNodeDescription result = converter.convert(gateway, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.DECISION);
        assertThat(result.getComponent()).isEqualTo(ForkComponent.NAME);
        assertThat(result.getConfig()).containsEntry(ForkComponent.CONF_FORK_MODE, ForkComponent.ForkMode.EXCLUSIVE);
        @SuppressWarnings("unchecked")
        Map<String, String> conditions = (Map<String, String>) result.getConfig().get(ForkComponent.CONF_CONDITIONS);
        assertThat(conditions).hasSize(2);
    }

    @Test
    void shouldSetDefaultPath_whenPresent() {
        // setup
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("test-process");
        model.addProcess(process);

        ExclusiveGateway gateway = new ExclusiveGateway();
        gateway.setId("gw1");
        process.addFlowElement(gateway);

        ManualTask task1 = new ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        ManualTask task2 = new ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        SequenceFlow flow1 = new SequenceFlow();
        flow1.setId("flow1");
        flow1.setSourceRef("gw1");
        flow1.setTargetRef("task1");
        flow1.setConditionExpression("${x > 10}");
        process.addFlowElement(flow1);
        gateway.getOutgoingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setId("flow2");
        flow2.setSourceRef("gw1");
        flow2.setTargetRef("task2");
        process.addFlowElement(flow2);
        gateway.getOutgoingFlows().add(flow2);

        gateway.setDefaultFlow("flow2");

        // expectation
        WorkflowNodeDescription result = converter.convert(gateway, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).containsEntry(ForkComponent.CONF_DEFAULT_PATH, "manualTask#task2");
    }

    @Test
    void shouldCanConvert_forExclusiveGateway() {
        // setup
        ExclusiveGateway gateway = new ExclusiveGateway();

        // expectation
        boolean result = converter.canConvert(gateway);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotCanConvert_forOtherElements() {
        // setup
        ServiceTask serviceTask = new ServiceTask();

        // expectation
        boolean result = converter.canConvert(serviceTask);

        // validation
        assertThat(result).isFalse();
    }
}
