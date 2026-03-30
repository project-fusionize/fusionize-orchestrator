package dev.fusionize.process.converters.tasks;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTaskConverterTest {

    private final ServiceTaskConverter converter = new ServiceTaskConverter();

    @Test
    void shouldConvertWithImplementation() {
        // setup
        ServiceTask task = new ServiceTask();
        task.setId("svc1");
        task.setImplementation("myService");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getComponent()).isEqualTo("myService");
    }

    @Test
    void shouldFallbackToNoop_whenNoImplementation() {
        // setup
        ServiceTask task = new ServiceTask();
        task.setId("svc1");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result.getComponent()).isEqualTo("noop");
    }

    @Test
    void shouldSetTypeToTask() {
        // setup
        ServiceTask task = new ServiceTask();
        task.setId("svc1");

        // expectation
        WorkflowNodeDescription result = converter.convert(task, new BpmnModel());

        // validation
        assertThat(result.getType()).isEqualTo(WorkflowNodeType.TASK);
    }

    @Test
    void shouldCanConvert_forServiceTask() {
        // setup
        ServiceTask task = new ServiceTask();

        // expectation
        boolean result = converter.canConvert(task);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotCanConvert_forOtherElements() {
        // setup
        ScriptTask scriptTask = new ScriptTask();

        // expectation
        boolean result = converter.canConvert(scriptTask);

        // validation
        assertThat(result).isFalse();
    }
}
