package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignalEventDefinitionConverterTest {

    @Test
    void testConvert() {
        SignalEventDefinitionConverter converter = new SignalEventDefinitionConverter(
                dev.fusionize.workflow.WorkflowNodeType.START);
        SignalEventDefinition definition = new SignalEventDefinition();
        definition.setSignalRef("signalRef");

        WorkflowNodeDescription node = converter.convert(definition, new BpmnModel());

        assertNotNull(node);
        assertEquals("signal", node.getComponent());
        assertEquals("signalRef", node.getConfig().get("signalRef"));
    }

    @Test
    void testCanConvert() {
        SignalEventDefinitionConverter converter = new SignalEventDefinitionConverter(
                dev.fusionize.workflow.WorkflowNodeType.START);
        assertTrue(converter.canConvert(new SignalEventDefinition()));
    }
}
