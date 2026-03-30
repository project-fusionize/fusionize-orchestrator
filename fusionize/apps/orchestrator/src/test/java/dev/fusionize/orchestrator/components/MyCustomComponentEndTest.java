package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MyCustomComponentEndTest {

    @Mock
    ComponentUpdateEmitter emitter;

    @Mock
    Context context;

    @Mock
    ComponentRuntimeConfig config;

    @Test
    void shouldConfigureWithoutError() {
        // setup
        var component = new MyCustomComponentEnd();

        // expectation & validation
        assertThatCode(() -> component.configure(config)).doesNotThrowAnyException();
    }

    @Test
    void shouldCallSuccessOnCanActivate() {
        // setup
        var component = new MyCustomComponentEnd();

        // expectation
        component.canActivate(context, emitter);

        // validation
        verify(emitter).success(context);
    }
}
