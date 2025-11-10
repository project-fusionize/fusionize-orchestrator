package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.workflow.WorkflowContext;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.StartComponentRuntime;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class MyCustomComponentRecEmail extends StartComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MyCustomComponentRecEmail.class);
    private final EmailBoxService emailBoxService;
    private String address;

    MyCustomComponentRecEmail(EventPublisher<Event> eventPublisher, EmailBoxService emailBoxService) {
        super(eventPublisher);
        this.emailBoxService = emailBoxService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.address = config.getConfig().get("address").toString();
    }

    @Override
    public void canActivate(ComponentActivatedEvent onActivate) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                logger.info("MockRecEmailComponentRuntime activated");
                onActivate.setException(null);
                publish(onActivate);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error(throwable.getMessage(), throwable);
            }
        });
    }

    @Override
    public void start(ComponentTriggeredEvent onTriggered) {
        // Continuously check inbox asynchronously
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    if(!emailBoxService.getInbox().isEmpty()){
                        logger.info("inbox size: {}", emailBoxService.getInbox().size());
                    }

                    if (!emailBoxService.getInbox().isEmpty()) {
                        // Remove the first email and process it
                        String email = emailBoxService.getInbox().remove(0);

                        String worklog = "MockRecEmailComponentRuntime handle email: " + email;
                        logger.info(worklog);

                        WorkflowContext ctx = onTriggered.getContext();
                        ctx.getData().put("email_message", email);

                        // Trigger downstream workflow components
                        publish(onTriggered);
                    }

                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing email", e);
                }
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error(throwable.getMessage(), throwable);
            }
        });
    }
}
