package dev.fusionize.process;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.ComponentConfig;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InclusiveGatewayConverterTest {

    @Test
    void testConvertAsJoin() {
        ProcessConverter converter = new ProcessConverter();
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("test-process");
        model.addProcess(process);

        InclusiveGateway gateway = new InclusiveGateway();
        gateway.setId("inclusiveJoin");
        process.addFlowElement(gateway);

        // Add 2 incoming flows
        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("task1");
        flow1.setTargetRef("inclusiveJoin");
        gateway.getIncomingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("task2");
        flow2.setTargetRef("inclusiveJoin");
        gateway.getIncomingFlows().add(flow2);

        // Mock source elements so they can be resolved
        org.flowable.bpmn.model.ManualTask task1 = new org.flowable.bpmn.model.ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        org.flowable.bpmn.model.ManualTask task2 = new org.flowable.bpmn.model.ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        Process fusionizeProcess = Process.of("test-process", "<xml/>", model);
        dev.fusionize.workflow.Workflow workflow = converter.convertToWorkflow(fusionizeProcess);

        assertNotNull(workflow);
        dev.fusionize.workflow.WorkflowNode node = findNodeByKey(workflow.getNodes(), "inclusiveGateway#inclusiveJoin");
        assertNotNull(node, "Node inclusiveGateway#inclusiveJoin not found");

        assertEquals(WorkflowNodeType.WAIT, node.getType());
        assertEquals("join", node.getComponent());

        ComponentConfig config = node.getComponentConfig();
        assertEquals("PICK_LAST", config.getConfig().get("mergeStrategy"));
        assertEquals("ALL", config.getConfig().get("waitMode"));

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

        InclusiveGateway gateway = new InclusiveGateway();
        gateway.setId("inclusiveFork");
        process.addFlowElement(gateway);

        // Add 2 outgoing flows
        SequenceFlow flow1 = new SequenceFlow();
        flow1.setSourceRef("inclusiveFork");
        flow1.setTargetRef("task1");
        flow1.setConditionExpression("${var > 10}");
        gateway.getOutgoingFlows().add(flow1);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setSourceRef("inclusiveFork");
        flow2.setTargetRef("task2");
        gateway.getOutgoingFlows().add(flow2);

        // Mock target elements
        org.flowable.bpmn.model.ManualTask task1 = new org.flowable.bpmn.model.ManualTask();
        task1.setId("task1");
        process.addFlowElement(task1);

        org.flowable.bpmn.model.ManualTask task2 = new org.flowable.bpmn.model.ManualTask();
        task2.setId("task2");
        process.addFlowElement(task2);

        Process fusionizeProcess = Process.of("test-process", "<xml/>", model);
        dev.fusionize.workflow.Workflow workflow = converter.convertToWorkflow(fusionizeProcess);

        assertNotNull(workflow);
        dev.fusionize.workflow.WorkflowNode node = findNodeByKey(workflow.getNodes(), "inclusiveGateway#inclusiveFork");
        assertNotNull(node, "Node inclusiveGateway#inclusiveFork not found");

        assertEquals(WorkflowNodeType.DECISION, node.getType());
        assertEquals("fork", node.getComponent());

        ComponentConfig config = node.getComponentConfig();
        Map<String, String> conditions = (Map<String, String>) config.getConfig().get("conditions");
        assertNotNull(conditions);
        assertEquals("${var > 10}", conditions.get("manualTask#task1"));
    }

    private dev.fusionize.workflow.WorkflowNode findNodeByKey(List<dev.fusionize.workflow.WorkflowNode> nodes,
            String key) {
        if (nodes == null)
            return null;
        for (dev.fusionize.workflow.WorkflowNode node : nodes) {
            if (key.equals(node.getWorkflowNodeKey())) {
                return node;
            }
            dev.fusionize.workflow.WorkflowNode found = findNodeByKey(node.getChildren(), key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
