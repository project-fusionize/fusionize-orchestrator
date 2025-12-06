package dev.fusionize.process.converters.events;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StartEventConverterTest {

    @Test
    void testConvertNoop() {
        StartEventConverter converter = new StartEventConverter();
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");

        WorkflowNodeDescription node = converter.convert(startEvent, new BpmnModel());

        assertNotNull(node);
        assertEquals(WorkflowNodeType.START, node.getType());
        assertEquals(NoopComponent.NAME, node.getComponent());
    }

    @Test
    void testConvertWithMessage() {
        StartEventConverter converter = new StartEventConverter();
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");

        MessageEventDefinition messageDefinition = new MessageEventDefinition();
        messageDefinition.setMessageRef("messageRef");
        startEvent.addEventDefinition(messageDefinition);

        WorkflowNodeDescription node = converter.convert(startEvent, new BpmnModel());

        assertNotNull(node);
        assertEquals(WorkflowNodeType.START, node.getType());
        assertEquals("message", node.getComponent());
        assertEquals("messageRef", node.getConfig().get("messageRef"));
    }

    @Test
    void testCanConvert() {
        StartEventConverter converter = new StartEventConverter();
        assertTrue(converter.canConvert(new StartEvent()));
    }
}
