package dev.fusionize.worker.stomp;

import dev.fusionize.workflow.LogListener;
import dev.fusionize.workflow.WorkflowLog;
import org.springframework.stereotype.Component;

@Component
public class StompLogListener implements LogListener {
    private static final String SEND_LOG_ENDPOINT = "/app/workflow/log";

    private final WorkerStompSessionHandler stompSessionHandler;

    public StompLogListener(WorkerStompSessionHandler stompSessionHandler) {
        this.stompSessionHandler = stompSessionHandler;
    }

    @Override
    public void onLog(WorkflowLog log) {
        stompSessionHandler.send(SEND_LOG_ENDPOINT, log);
    }
}
