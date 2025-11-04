package dev.fusionize.worker.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;

import java.lang.reflect.Type;

public class WorkerStompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WorkerStompSessionHandler.class);

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("Connected to STOMP server: {}", session.getSessionId());

        // Subscribe to a topic
        session.subscribe("/topic/messages", this);

        // Send a message
        session.send("/app/hello", "Hello from client!".getBytes());
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        logger.info("Received message: {}", payload);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        // You can deserialize to String, or any custom object
        return String.class;
    }

    @Override
    public void handleException(
            StompSession session,
            StompCommand command,
            StompHeaders headers,
            byte[] payload,
            Throwable exception
    ) {
        logger.error("Got an exception", exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        logger.error("Transport error in session {}", session.getSessionId(), exception);
    }
}
