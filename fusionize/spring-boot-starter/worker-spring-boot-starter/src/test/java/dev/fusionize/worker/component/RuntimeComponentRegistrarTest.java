package dev.fusionize.worker.component;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.registery.WorkflowComponentRegistry;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeComponentRegistrarTest {

    @Mock
    private WorkflowComponentRegistry componentRegistry;

    @Test
    void shouldValidateComponentFactory() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);

        // expectation
        boolean result = registrar.isValidComponentFactory(ComponentRuntimeFactory.class);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectInvalidComponentFactory() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);

        // expectation
        boolean result = registrar.isValidComponentFactory(String.class);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldValidateComponentDefinition() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);
        RuntimeComponentDefinition definition = mock(RuntimeComponentDefinition.class);
        when(definition.name()).thenReturn("test-component");
        when(definition.description()).thenReturn("A test component");
        when(definition.domain()).thenReturn("test-domain");

        // expectation
        boolean result = registrar.isValidComponentDefinition(definition);

        // validation
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectInvalidComponentDefinition_nullDef() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);

        // expectation
        boolean result = registrar.isValidComponentDefinition(null);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectInvalidComponentDefinition_emptyName() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);
        RuntimeComponentDefinition definition = mock(RuntimeComponentDefinition.class);
        when(definition.name()).thenReturn("");
        when(definition.domain()).thenReturn("test-domain");

        // expectation
        boolean result = registrar.isValidComponentDefinition(definition);

        // validation
        assertThat(result).isFalse();
    }

    @Test
    void shouldRegisterComponent() {
        // setup
        var registrar = new RuntimeComponentRegistrar(componentRegistry);
        RuntimeComponentDefinition definition = mock(RuntimeComponentDefinition.class);
        when(definition.name()).thenReturn("test-component");
        when(definition.description()).thenReturn("A test component");
        when(definition.domain()).thenReturn("custom-domain");
        when(definition.actors()).thenReturn(new Actor[]{Actor.SYSTEM});

        WorkflowComponent expectedComponent = WorkflowComponent.builder("")
                .withName("test-component")
                .withDescription("A test component")
                .withDomain("custom-domain")
                .build();
        when(componentRegistry.register(any(WorkflowComponent.class))).thenReturn(expectedComponent);

        // expectation
        WorkflowComponent result = registrar.registerComponent(definition);

        // validation
        ArgumentCaptor<WorkflowComponent> captor = ArgumentCaptor.forClass(WorkflowComponent.class);
        verify(componentRegistry).register(captor.capture());
        WorkflowComponent registered = captor.getValue();
        assertThat(registered.getName()).isEqualTo("test-component");
        assertThat(registered.getDescription()).isEqualTo("A test component");
        assertThat(registered.getDomain()).isEqualTo("custom-domain");
        assertThat(result).isEqualTo(expectedComponent);
    }
}
