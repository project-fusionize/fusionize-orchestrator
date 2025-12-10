package dev.fusionize.process;

import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.StartEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessConverterAnnotateTest {

    @Test
    void testAnnotate() {
        ProcessConverter converter = new ProcessConverter();
        Process process = Process.of("test-process", "<xml/>", new BpmnModel());

        String yaml = """
                startEvent#start:
                  actor: start
                  component: test.receivedIncomingEmail
                  config:
                    address: incoming@fusionize.dev
                    address2: incoming2@fusionize.dev

                serviceTask#salesRoute:
                  actor: task
                  component: test.sendEmail
                  config:
                    address: sales-team@fusionize.dev
                """;

        converter.annotate(process, yaml);

        assertEquals(yaml, process.getBpmnSupportYaml());
        assertNotNull(process.getDefinitions());
        assertEquals(2, process.getDefinitions().size());

        Map<String, WorkflowNodeDescription> definitions = process.getDefinitions();
        assertTrue(definitions.containsKey("startEvent#start"));
        assertTrue(definitions.containsKey("serviceTask#salesRoute"));

        WorkflowNodeDescription startNode = definitions.get("startEvent#start");
        assertEquals("start", startNode.getActor());
        assertEquals("test.receivedIncomingEmail", startNode.getComponent());
        assertEquals("incoming@fusionize.dev", startNode.getConfig().get("address"));
        assertEquals("incoming2@fusionize.dev", startNode.getConfig().get("address2"));

        WorkflowNodeDescription salesNode = definitions.get("serviceTask#salesRoute");
        assertEquals("task", salesNode.getActor());
        assertEquals("test.sendEmail", salesNode.getComponent());
        assertEquals("sales-team@fusionize.dev", salesNode.getConfig().get("address"));
    }

    @Test
    void testAnnotateWithNullYaml() {
        ProcessConverter converter = new ProcessConverter();
        Process process = Process.of("test-process", "<xml/>", new BpmnModel());

        converter.annotate(process, null);

        assertNull(process.getBpmnSupportYaml());
        assertNull(process.getDefinitions());
    }

    @Test
    void testAnnotateWithEmptyYaml() {
        ProcessConverter converter = new ProcessConverter();
        Process process = Process.of("test-process", "<xml/>", new BpmnModel());

        converter.annotate(process, "   ");

        assertEquals("   ", process.getBpmnSupportYaml());
        assertNull(process.getDefinitions());
    }

    @Test
    void testConvertToWorkflowWithAnnotation() {
        ProcessConverter converter = new ProcessConverter();
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process bpmnProcess = new org.flowable.bpmn.model.Process();
        bpmnProcess.setId("test-process");
        bpmnProcess.setName("Test Process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        bpmnProcess.addFlowElement(startEvent);

        model.addProcess(bpmnProcess);

        Process process = Process.of("test-process", "<xml/>", model);

        String yaml = """
                startEvent#start:
                  actor: start
                  component: test.overridden
                  config:
                    key: value
                """;

        converter.annotate(process, yaml);
        dev.fusionize.workflow.Workflow workflow = converter.convertToWorkflow(process);

        assertNotNull(workflow);
        List<dev.fusionize.workflow.WorkflowNode> nodes = workflow.getNodes();

        dev.fusionize.workflow.WorkflowNode startNode = nodes.stream()
                .filter(n -> "startEvent#start".equals(n.getWorkflowNodeKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(startNode);
        assertEquals("start:test.overridden", startNode.getComponent());
        assertEquals("value", startNode.getComponentConfig().getConfig().get("key"));
    }
}
