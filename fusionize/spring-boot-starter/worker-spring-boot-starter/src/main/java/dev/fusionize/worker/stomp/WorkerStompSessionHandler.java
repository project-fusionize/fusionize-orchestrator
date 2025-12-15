package dev.fusionize.worker.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;

public class WorkerStompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WorkerStompSessionHandler.class);

    private StompSession session;

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        this.session = session;
        logger.info("Connected to STOMP server: {}", session.getSessionId());

        // Subscribe to a topic
        session.subscribe("/topic/messages", this);
    }

    public <T> void send(String destination, T payload) {
        if (session != null && session.isConnected()) {
            logger.debug("sending message to {}", destination);
            try {
                session.send(destination, payload);
                logger.debug("sent message to {}", destination);
            } catch (Exception e) {
                logger.error("Failed to send message to {}", destination, e);
            }
        } else {
            logger.warn("Cannot send message to {}, session is not connected", destination);
        }
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        logger.info("Received message: {}", payload);
    }

    @Override
    public void handleException(
            StompSession session,
            StompCommand command,
            StompHeaders headers,
            byte[] payload,
            Throwable exception) {
        logger.error("Got an exception", exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        if (exception instanceof ConnectionLostException) {
            logger.info("Connection closed: {}", exception.getMessage());
        } else {
            logger.error("Transport error in session {}", session.getSessionId(), exception);
        }
    }
}
