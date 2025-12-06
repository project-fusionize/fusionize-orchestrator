package dev.fusionize.process.converters.events;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndEventConverterTest {

    @Test
    void testConvertNoop() {
        EndEventConverter converter = new EndEventConverter();
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");

        WorkflowNodeDescription node = converter.convert(endEvent, new BpmnModel());

        assertNotNull(node);
        assertEquals(WorkflowNodeType.END, node.getType());
        assertEquals(NoopComponent.NAME, node.getComponent());
    }

    @Test
    void testConvertWithMessage() {
        EndEventConverter converter = new EndEventConverter();
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");

        MessageEventDefinition messageDefinition = new MessageEventDefinition();
        messageDefinition.setMessageRef("messageRef");
        endEvent.addEventDefinition(messageDefinition);

        WorkflowNodeDescription node = converter.convert(endEvent, new BpmnModel());

        assertNotNull(node);
        assertEquals(WorkflowNodeType.END, node.getType());
        assertEquals("message", node.getComponent());
        assertEquals("messageRef", node.getConfig().get("messageRef"));
    }

    @Test
    void testCanConvert() {
        EndEventConverter converter = new EndEventConverter();
        assertTrue(converter.canConvert(new EndEvent()));
    }
}
