package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowContext;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.StartComponentRuntime;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RuntimeComponentDefinition()
public class MyCustomComponentRuntimeFactory implements ComponentRuntimeFactory {
    private final EventPublisher<Event> eventPublisher;
    private final List<String> inbox = new CopyOnWriteArrayList<>();

    public MyCustomComponentRuntimeFactory(EventPublisher<Event> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ComponentRuntime create() {
        return new MockRecEmailComponentRuntime(eventPublisher, inbox);
    }

    public static final class MockRecEmailComponentRuntime extends StartComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockRecEmailComponentRuntime.class);
        private final List<String> inbox;
        private String address;

        MockRecEmailComponentRuntime(EventPublisher<Event> eventPublisher, List<String> inbox) {
            super(eventPublisher);
            this.inbox = inbox;
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
                        logger.info("inbox size: {}", inbox.size());

                        if (!inbox.isEmpty()) {
                            // Remove the first email and process it
                            String email = inbox.remove(0);

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

}
