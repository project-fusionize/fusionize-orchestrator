package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendEmailTest {

    @Mock
    ComponentUpdateEmitter emitter;

    @Mock
    ComponentUpdateEmitter.Logger logger;

    @Mock
    Context context;

    @Mock
    ComponentRuntimeConfig config;

    @Test
    void shouldConfigureWithoutError() {
        // setup
        var sendEmail = new SendEmail();

        // expectation & validation
        assertThatCode(() -> sendEmail.configure(config)).doesNotThrowAnyException();
    }

    @Test
    void shouldCallSuccessOnCanActivate() {
        // setup
        var sendEmail = new SendEmail();
        when(emitter.logger()).thenReturn(logger);

        // expectation
        sendEmail.canActivate(context, emitter);

        // validation
        verify(logger).debug("MockSndEmailComponent activated");
        verify(emitter).success(context);
    }

    @Test
    void shouldCallSuccessOnRun() {
        // setup
        var sendEmail = new SendEmail();
        when(emitter.logger()).thenReturn(logger);

        // expectation
        new Thread(() -> sendEmail.run(context, emitter)).start();

        // validation
        verify(emitter, timeout(2000)).success(context);
        verify(logger, timeout(2000)).warn("MockSndEmailComponent finished");
    }
}
