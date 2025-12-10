package dev.fusionize.process;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.ComponentConfig;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ComplexGateway;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComplexGatewayConverterTest {

    @Test
    void testConvertAsJoin() {
        ProcessConverter converter = new ProcessConverter();
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("test-process");
        model.addProcess(process);

        ComplexGateway gateway = new ComplexGateway();
        gateway.setId("complexJoin");
        process.addFlowElement(gateway);

        // Add 2 incoming flows
        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("task1");
        flow1.setTargetRef("complexJoin");
        gateway.getIncomingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("task2");
        flow2.setTargetRef("complexJoin");
        gateway.getIncomingFlows().add(flow2);

        // Mock source elements so they can be resolved
        ManualTask task1 = new ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        ManualTask task2 = new ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        Process fusionizeProcess = Process.of("test-process", "<xml/>", model);
        Workflow workflow = converter.convertToWorkflow(fusionizeProcess);

        assertNotNull(workflow);
        WorkflowNode node = findNodeByKey(workflow.getNodes(), "complexGateway#complexJoin");
        assertNotNull(node, "Node complexGateway#complexJoin not found");

        assertEquals(WorkflowNodeType.WAIT, node.getType());
        assertEquals("join", node.getComponent());

        ComponentConfig config = node.getComponentConfig();
        assertEquals("PICK_LAST", config.getConfig().get("mergeStrategy"));
        assertEquals("THRESHOLD", config.getConfig().get("waitMode"));
        assertEquals(1, config.getConfig().get("thresholdCount"));

        List<String> await = (List<String>) config.getConfig().get("await");
        assertNotNull(await);
        assertEquals(2, await.size());
        assertTrue(await.contains("manualTask#task1"));
        assertTrue(await.contains("manualTask#task2"));
    }

    @Test
    void testConvertAsFork() {
        ProcessConverter converter = new ProcessConverter();
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("test-process");
        model.addProcess(process);

        ComplexGateway gateway = new ComplexGateway();
        gateway.setId("complexFork");
        process.addFlowElement(gateway);

        // Add 2 outgoing flows
        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("complexFork");
        flow1.setTargetRef("task1");
        flow1.setConditionExpression("${var > 10}");
        gateway.getOutgoingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("complexFork");
        flow2.setTargetRef("task2");
        gateway.getOutgoingFlows().add(flow2);

        // Mock target elements
        ManualTask task1 = new ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        ManualTask task2 = new ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        Process fusionizeProcess = Process.of("test-process", "<xml/>", model);
        Workflow workflow = converter.convertToWorkflow(fusionizeProcess);

        assertNotNull(workflow);
        WorkflowNode node = findNodeByKey(workflow.getNodes(), "complexGateway#complexFork");
        assertNotNull(node, "Node complexGateway#complexFork not found");

        assertEquals(WorkflowNodeType.DECISION, node.getType());
        assertEquals("fork", node.getComponent());

        ComponentConfig config = node.getComponentConfig();
        Map<String, String> conditions = (Map<String, String>) config.getConfig().get("conditions");
        assertNotNull(conditions);
        assertEquals("${var > 10}", conditions.get("manualTask#task1"));
    }

    private WorkflowNode findNodeByKey(List<WorkflowNode> nodes,
                                       String key) {
        if (nodes == null)
            return null;
        for (WorkflowNode node : nodes) {
            if (key.equals(node.getWorkflowNodeKey())) {
                return node;
            }
            WorkflowNode found = findNodeByKey(node.getChildren(), key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
