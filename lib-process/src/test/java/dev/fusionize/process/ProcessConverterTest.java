package dev.fusionize.process;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessConverterTest {
    ProcessConverter processConverter;

    @BeforeEach
    void setUp() {
        processConverter = new ProcessConverter();
    }

    @Test
    void convert() throws IOException, XMLStreamException {
        URL bpmnUrl = this.getClass().getResource("/master_diagram.bpmn");
        assertNotNull(bpmnUrl);
        String xml = Files.readString(new File(bpmnUrl.getFile()).toPath());
        Process process = new ProcessConverter().convert(xml);
        Workflow workflow = processConverter.convertToWorkflow(process);
        assertNotNull(workflow);

        // Verify Start Event (Message)
        WorkflowNode startNode = findNode(workflow, "startEvent#Event_09q8evq");
        assertNotNull(startNode);
        assertEquals(WorkflowNodeType.START, startNode.getType());
        assertEquals("message", startNode.getComponent());
        assertEquals("Message_32h99mr", startNode.getComponentConfig().getConfig().get("messageRef"));

        // Verify Intermediate Catch Event (Timer)
        WorkflowNode timerNode = findNode(workflow, "intermediateCatchEvent#Event_04xvrr6");
        assertNotNull(timerNode);
        assertEquals(WorkflowNodeType.WAIT, timerNode.getType());
        assertEquals("delay", timerNode.getComponent());
        // Delay might be missing because timeDuration is null in the XML

        // Verify End Event
        WorkflowNode endNode = findNode(workflow, "endEvent#Event_0vlopqs");
        assertNotNull(endNode);
        assertEquals(WorkflowNodeType.END, endNode.getType());
        assertEquals(NoopComponent.NAME, endNode.getComponent());

        // Verify Script Task
        WorkflowNode scriptNode = findNode(workflow, "scriptTask#Activity_1puc8u1");
        assertNotNull(scriptNode);
        assertEquals(WorkflowNodeType.TASK, scriptNode.getType());
        assertEquals("script", scriptNode.getComponent());
        assertEquals("js", scriptNode.getComponentConfig().getConfig().get("parser"));

        // Verify Service Task
        WorkflowNode serviceNode = findNode(workflow, "serviceTask#Activity_19yr3yd");
        assertNotNull(serviceNode);
        assertEquals(WorkflowNodeType.TASK, serviceNode.getType());

        // Verify Exclusive Gateway (Fork)
        WorkflowNode forkNode = findNode(workflow, "exclusiveGateway#Gateway_1kvy8pa");
        assertNotNull(forkNode);
        assertEquals(WorkflowNodeType.DECISION, forkNode.getType());
        assertEquals("fork", forkNode.getComponent());

        // Verify Parallel Gateway (Join)
        WorkflowNode joinNode = findNode(workflow, "parallelGateway#Gateway_1s9zl65");
        assertNotNull(joinNode);
        assertEquals(WorkflowNodeType.WAIT, joinNode.getType());
        assertEquals("join", joinNode.getComponent());
    }

    private WorkflowNode findNode(Workflow workflow, String key) {
        return findNodeRecursive(workflow.getNodes(), key);
    }

    private WorkflowNode findNodeRecursive(List<WorkflowNode> nodes, String key) {
        if (nodes == null)
            return null;
        for (WorkflowNode node : nodes) {
            if (key.equals(node.getWorkflowNodeKey())) {
                return node;
            }
            WorkflowNode found = findNodeRecursive(node.getChildren(), key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}