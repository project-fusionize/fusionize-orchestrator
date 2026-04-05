package dev.fusionize.process.converters.gateways;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.JoinComponent;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelGatewayConverterTest {

    private final ParallelGatewayConverter converter = new ParallelGatewayConverter();

    @Test
    void shouldConvertForkAsNoopTask() {
        // setup
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("test-process");
        model.addProcess(process);

        ParallelGateway gateway = new ParallelGateway();
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
        gateway.getOutgoingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("gw1");
        flow2.setTargetRef("task2");
        gateway.getOutgoingFlows().add(flow2);

        // expectation
        WorkflowNodeDescription result = converter.convert(gateway, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.TASK);
        assertThat(result.getComponent()).isEqualTo(NoopComponent.NAME);
    }

    @Test
    void shouldConvertJoinWithAllWaitMode() {
        // setup
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("test-process");
        model.addProcess(process);

        ParallelGateway gateway = new ParallelGateway();
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
        assertThat(result.getConfig()).containsEntry(JoinComponent.CONF_WAIT_MODE, JoinComponent.WaitMode.ALL.toString());
        assertThat(result.getConfig()).containsEntry(JoinComponent.CONF_MERGE_STRATEGY, JoinComponent.MergeStrategy.PICK_LAST.toString());
        @SuppressWarnings("unchecked")
        List<String> await = (List<String>) result.getConfig().get(JoinComponent.CONF_AWAIT);
        assertThat(await).hasSize(2);
    }

    @Test
    void shouldCanConvert_forParallelGateway() {
        // setup
        ParallelGateway gateway = new ParallelGateway();

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
