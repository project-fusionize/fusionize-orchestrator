package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JoinComponentFactoryTest {

    @Mock
    private WorkflowExecutionRegistry workflowExecutionRegistry;

    @Test
    void shouldReturnCorrectName() {
        // setup
        var factory = new JoinComponentFactory(workflowExecutionRegistry);

        // expectation
        var name = factory.getName();

        // validation
        assertThat(name).isEqualTo("join");
    }

    @Test
    void shouldCreateJoinComponent() {
        // setup
        var factory = new JoinComponentFactory(workflowExecutionRegistry);

        // expectation
        var component = factory.create();

        // validation
        assertThat(component).isInstanceOf(JoinComponent.class);
    }

    @Test
    void shouldDescribeComponent() {
        // setup
        var factory = new JoinComponentFactory(workflowExecutionRegistry);

        // expectation
        var description = factory.describe();

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getDomain()).isEqualTo("join");
        assertThat(description.getName()).isEqualTo("join");
        assertThat(description.getDescription()).contains("JoinComponent");
    }
}
