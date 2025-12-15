package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.concurrent.CompletableFuture;
import dev.fusionize.common.utility.Debouncer;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import dev.fusionize.workflow.events.OrchestrationEvent;
import dev.fusionize.workflow.WorkflowExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static dev.fusionize.orchestrator.config.WebSocketConfig.TOPIC_BASE;

@Component
public class WorkflowUpdatePublisher {
    private static final Logger log = LoggerFactory.getLogger(WorkflowUpdatePublisher.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutionRegistry workflowExecutionRegistry;
    private final EventListener<Event> eventListener;
    private final Executor executor;
    Debouncer<String> debouncer = new Debouncer<>(300, TimeUnit.MILLISECONDS);

    public WorkflowUpdatePublisher(SimpMessagingTemplate messagingTemplate,
                                   WorkflowRegistry workflowRegistry,
                                   WorkflowExecutionRegistry workflowExecutionRegistry,
                                   EventListener<Event> eventListener,
                                   @Qualifier("workflowUpdateExecutor") Executor executor) {
        this.messagingTemplate = messagingTemplate;
        this.workflowRegistry = workflowRegistry;
        this.workflowExecutionRegistry = workflowExecutionRegistry;
        this.eventListener = eventListener;
        this.executor = executor;
        eventListener.addListener(this::onEvent);
    }


    public void onEvent(Event event) {
        if (event instanceof OrchestrationEvent orchestrationEvent) {
             debouncer.debounce(orchestrationEvent.getWorkflowExecutionId(), () ->
                 CompletableFuture.runAsync(() -> {
                     try {
                         WorkflowExecution workflowExecution = workflowExecutionRegistry.getWorkflowExecution(
                                 orchestrationEvent.getWorkflowExecutionId());
                         if (workflowExecution != null) {
                             messagingTemplate.convertAndSend(
                                     TOPIC_BASE +".workflow-executions." + workflowExecution.getWorkflowId()
                                     , workflowExecution);
                         }
                     } catch (Exception e) {
                         log.error("Error sending workflow update: {}", e.getMessage());
                     }
                 }, executor)
             );
        }
    }

}
