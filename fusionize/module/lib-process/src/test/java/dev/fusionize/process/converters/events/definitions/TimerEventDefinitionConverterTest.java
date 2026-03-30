package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimerEventDefinitionConverterTest {

    private final TimerEventDefinitionConverter converter = new TimerEventDefinitionConverter();

    @Test
    void shouldConvertWithDuration() {
        // setup
        TimerEventDefinition definition = new TimerEventDefinition();
        definition.setTimeDuration("PT5S");

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).containsEntry(DelayComponent.CONF_DELAY, 5000L);
    }

    @Test
    void shouldConvertWithoutDuration() {
        // setup
        TimerEventDefinition definition = new TimerEventDefinition();

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).doesNotContainKey(DelayComponent.CONF_DELAY);
    }

    @Test
    void shouldThrow_forInvalidDuration() {
        // setup
        TimerEventDefinition definition = new TimerEventDefinition();
        definition.setTimeDuration("invalid");

        // expectation & validation
        assertThrows(IllegalArgumentException.class, () -> converter.convert(definition, new BpmnModel()));
    }

    @Test
    void shouldSetComponentToDelay() {
        // setup
        TimerEventDefinition definition = new TimerEventDefinition();

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result.getComponent()).isEqualTo(DelayComponent.NAME);
    }

    @Test
    void shouldCanConvert_forTimerEventDefinition() {
        // setup
        TimerEventDefinition definition = new TimerEventDefinition();

        // expectation
        boolean result = converter.canConvert(definition);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotCanConvert_forOtherDefinitions() {
        // setup
        MessageEventDefinition definition = new MessageEventDefinition();

        // expectation
        boolean result = converter.canConvert(definition);

        // validation
        assertThat(result).isFalse();
    }
}
