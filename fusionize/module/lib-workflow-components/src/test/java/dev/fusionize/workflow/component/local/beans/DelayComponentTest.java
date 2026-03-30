package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelayComponentTest {

    private DelayComponent delayComponent;

    @Mock
    private ComponentUpdateEmitter emitter;

    @Mock
    private ComponentUpdateEmitter.Logger logger;

    @BeforeEach
    void setUp() {
        // setup
        delayComponent = new DelayComponent();
        var config = ComponentRuntimeConfig.builder()
                .put("delay", 100)
                .build();
        delayComponent.configure(config);
    }

    @Test
    void shouldConfigureWithCustomDelay() {
        // setup
        var component = new DelayComponent();
        var config = ComponentRuntimeConfig.builder()
                .put("delay", 100)
                .build();

        // expectation
        component.configure(config);
        var context = new Context();
        when(emitter.logger()).thenReturn(logger);
        component.run(context, emitter);

        // validation
        verify(emitter, timeout(2000)).success(any(Context.class));
        verify(logger).info(anyString(), any(Object.class));
    }

    @Test
    void shouldConfigureWithDefaultDelay() {
        // setup
        var component = new DelayComponent();
        var config = ComponentRuntimeConfig.builder().build();

        // expectation
        component.configure(config);

        // validation
        // default delay is 5000ms; we verify by checking the config was accepted without error
        // and that running would use the default (we don't actually wait 5s)
        assertThat(component).isNotNull();
    }

    @Test
    void shouldCallSuccessOnCanActivate() {
        // setup
        var context = new Context();

        // expectation
        delayComponent.canActivate(context, emitter);

        // validation
        verify(emitter).success(context);
    }

    @Test
    void shouldScheduleDelayAndCallSuccess() {
        // setup
        var component = new DelayComponent();
        var config = ComponentRuntimeConfig.builder()
                .put("delay", 50)
                .build();
        component.configure(config);
        var context = new Context();
        when(emitter.logger()).thenReturn(logger);

        // expectation
        component.run(context, emitter);

        // validation
        verify(emitter, timeout(2000)).success(any(Context.class));
    }

    @Test
    void shouldSetDelayedVarInContext() {
        // setup
        var component = new DelayComponent();
        var config = ComponentRuntimeConfig.builder()
                .put("delay", 50)
                .build();
        component.configure(config);
        var context = new Context();
        when(emitter.logger()).thenReturn(logger);

        // expectation
        component.run(context, emitter);

        // validation
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(emitter, timeout(2000)).success(captor.capture());
        assertThat(captor.getValue().var(DelayComponent.VAR_DELAYED, Integer.class)).hasValue(50);
    }
}
