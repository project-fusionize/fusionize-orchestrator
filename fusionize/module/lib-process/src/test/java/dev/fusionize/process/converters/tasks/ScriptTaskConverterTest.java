package dev.fusionize.process.converters.tasks;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.local.beans.ScriptComponent;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptTaskConverterTest {

    private final ScriptTaskConverter converter = new ScriptTaskConverter();

    @Test
    void shouldConvertScriptTask() {
        // setup
        ScriptTask task = new ScriptTask();
        task.setId("script1");
        task.setScriptFormat("javascript");
        task.setScript("var x=1;");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).containsEntry(ScriptComponent.CONF_PARSER, "javascript");
        assertThat(result.getConfig()).containsEntry(ScriptComponent.CONF_SCRIPT, "var x=1;");
    }

    @Test
    void shouldSetComponentToScript() {
        // setup
        ScriptTask task = new ScriptTask();
        task.setId("script1");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result.getComponent()).isEqualTo(ScriptComponent.NAME);
    }

    @Test
    void shouldSetTypeToTask() {
        // setup
        ScriptTask task = new ScriptTask();
        task.setId("script1");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.TASK);
    }

    @Test
    void shouldCanConvert_forScriptTask() {
        // setup
        ScriptTask task = new ScriptTask();

        // expectation
        boolean result = converter.canConvert(task);

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
