package dev.fusionize.process.converters.tasks;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ManualTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManualTaskConverterTest {

    @Test
    void testConvert() {
        ManualTaskConverter converter = new ManualTaskConverter();
        ManualTask task = new ManualTask();
        task.setId("manualTask");

        WorkflowNodeDescription node = converter.convert(task, new BpmnModel());

        assertNotNull(node);
        assertEquals(WorkflowNodeType.TASK, node.getType());
    }

    @Test
    void testCanConvert() {
        ManualTaskConverter converter = new ManualTaskConverter();
        assertTrue(converter.canConvert(new ManualTask()));
    }
}
