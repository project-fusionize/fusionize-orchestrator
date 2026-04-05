package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoopComponentTest {

    private NoopComponent noopComponent;

    @Mock
    private ComponentUpdateEmitter emitter;

    @BeforeEach
    void setUp() {
        // setup
        noopComponent = new NoopComponent();
    }

    @Test
    void shouldConfigureWithoutError() {
        // setup
        var config = ComponentRuntimeConfig.builder().build();

        // expectation
        // validation
        assertThatCode(() -> noopComponent.configure(config)).doesNotThrowAnyException();
    }

    @Test
    void shouldCallSuccessOnCanActivate() {
        // setup
        var context = new Context();

        // expectation
        noopComponent.canActivate(context, emitter);

        // validation
        verify(emitter).success(context);
    }

    @Test
    void shouldCallSuccessOnRun() {
        // setup
        var context = new Context();

        // expectation
        noopComponent.run(context, emitter);

        // validation
        verify(emitter).success(context);
    }

    @Test
    void shouldPassContextThrough_onCanActivate() {
        // setup
        var context = new Context();
        context.set("testKey", "testValue");

        // expectation
        noopComponent.canActivate(context, emitter);

        // validation
        verify(emitter).success(context);
        assertThat(context.varString("testKey")).hasValue("testValue");
    }

    @Test
    void shouldPassContextThrough_onRun() {
        // setup
        var context = new Context();
        context.set("testKey", "testValue");

        // expectation
        noopComponent.run(context, emitter);

        // validation
        verify(emitter).success(context);
        assertThat(context.varString("testKey")).hasValue("testValue");
    }
}
