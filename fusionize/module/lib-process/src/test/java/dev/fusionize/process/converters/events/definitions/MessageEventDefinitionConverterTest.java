package dev.fusionize.process.converters.events.definitions;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEventDefinitionConverterTest {

    @Test
    void shouldConvertWithMessageRef() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.WAIT);
        MessageEventDefinition definition = new MessageEventDefinition();
        definition.setMessageRef("myMessageRef");

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).containsEntry("messageRef", "myMessageRef");
    }

    @Test
    void shouldConvertWithoutMessageRef() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.WAIT);
        MessageEventDefinition definition = new MessageEventDefinition();

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).doesNotContainKey("messageRef");
    }

    @Test
    void shouldSetCorrectNodeType() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.WAIT);
        MessageEventDefinition definition = new MessageEventDefinition();

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.WAIT);
    }

    @Test
    void shouldSetComponentToMessage() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.START);
        MessageEventDefinition definition = new MessageEventDefinition();

        // expectation
        WorkflowNodeDescription result = converter.convert(definition, new BpmnModel());

        // validation
        assertThat(result.getComponent()).isEqualTo("message");
    }

    @Test
    void shouldCanConvert_forMessageEventDefinition() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.WAIT);

        // expectation
        boolean result = converter.canConvert(new MessageEventDefinition());

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotCanConvert_forOtherDefinitions() {
        // setup
        MessageEventDefinitionConverter converter = new MessageEventDefinitionConverter(WorkflowNodeType.WAIT);

        // expectation
        boolean result = converter.canConvert(new TimerEventDefinition());

        // validation
        assertThat(result).isFalse();
    }
}
