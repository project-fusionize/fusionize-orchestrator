package dev.fusionize.worker.stomp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerStompSessionHandlerTest {

    @Mock
    private StompSession session;

    @Test
    void shouldStoreSessionAfterConnected() {
        // setup
        var handler = new WorkerStompSessionHandler();
        when(session.isConnected()).thenReturn(true);

        // expectation
        handler.afterConnected(session, new StompHeaders());
        handler.send("/test/destination", "test payload");

        // validation
        verify(session).send("/test/destination", "test payload");
    }

    @Test
    void shouldSubscribeToTopic_afterConnected() {
        // setup
        var handler = new WorkerStompSessionHandler();

        // expectation
        handler.afterConnected(session, new StompHeaders());

        // validation
        verify(session).subscribe(eq("/topic/messages"), eq(handler));
    }

    @Test
    void shouldSendPayload_whenConnected() {
        // setup
        var handler = new WorkerStompSessionHandler();
        when(session.isConnected()).thenReturn(true);
        handler.afterConnected(session, new StompHeaders());

        // expectation
        handler.send("/app/test", "payload");

        // validation
        verify(session).send("/app/test", "payload");
    }

    @Test
    void shouldNotSend_whenSessionNotConnected() {
        // setup
        var handler = new WorkerStompSessionHandler();
        when(session.isConnected()).thenReturn(false);
        handler.afterConnected(session, new StompHeaders());

        // expectation
        handler.send("/app/test", "payload");

        // validation
        verify(session, never()).send(any(String.class), any());
    }

    @Test
    void shouldHandleExceptionWithoutThrowing() {
        // setup
        var handler = new WorkerStompSessionHandler();
        var exception = new RuntimeException("test error");

        // expectation & validation
        assertThatCode(() -> handler.handleException(
                session, StompCommand.SEND, new StompHeaders(),
                new byte[0], exception
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleTransportErrorWithoutThrowing() {
        // setup
        var handler = new WorkerStompSessionHandler();
        when(session.getSessionId()).thenReturn("test-session-id");
        var exception = new RuntimeException("transport error");

        // expectation & validation
        assertThatCode(() -> handler.handleTransportError(session, exception))
                .doesNotThrowAnyException();
    }
}
