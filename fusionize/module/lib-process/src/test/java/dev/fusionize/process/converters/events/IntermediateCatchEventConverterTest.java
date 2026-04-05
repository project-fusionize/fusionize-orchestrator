package dev.fusionize.process.converters.events;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.component.local.beans.NoopComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateCatchEventConverterTest {

    private final IntermediateCatchEventConverter converter = new IntermediateCatchEventConverter();

    @Test
    void shouldConvertTimerEventDefinition() {
        // setup
        IntermediateCatchEvent event = new IntermediateCatchEvent();
        event.setId("ice1");
        TimerEventDefinition timerDef = new TimerEventDefinition();
        timerDef.setTimeDuration("PT5S");
        event.addEventDefinition(timerDef);
        BpmnModel model = new BpmnModel();

        // expectation
        WorkflowNodeDescription result = converter.convert(event, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.WAIT);
        assertThat(result.getComponent()).isEqualTo(DelayComponent.NAME);
        assertThat(result.getConfig()).containsEntry(DelayComponent.CONF_DELAY, 5000L);
    }

    @Test
    void shouldConvertMessageEventDefinition() {
        // setup
        IntermediateCatchEvent event = new IntermediateCatchEvent();
        event.setId("ice2");
        MessageEventDefinition msgDef = new MessageEventDefinition();
        msgDef.setMessageRef("myMessage");
        event.addEventDefinition(msgDef);
        BpmnModel model = new BpmnModel();

        // expectation
        WorkflowNodeDescription result = converter.convert(event, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.WAIT);
        assertThat(result.getComponent()).isEqualTo("message");
        assertThat(result.getConfig()).containsEntry("messageRef", "myMessage");
    }

    @Test
    void shouldFallbackToNoop_whenNoSupportedDefinition() {
        // setup
        IntermediateCatchEvent event = new IntermediateCatchEvent();
        event.setId("ice3");
        BpmnModel model = new BpmnModel();

        // expectation
        WorkflowNodeDescription result = converter.convert(event, model);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.WAIT);
        assertThat(result.getComponent()).isEqualTo(NoopComponent.NAME);
    }

    @Test
    void shouldCanConvert_forIntermediateCatchEvent() {
        // setup
        IntermediateCatchEvent event = new IntermediateCatchEvent();

        // expectation
        boolean result = converter.canConvert(event);

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
