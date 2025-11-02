package dev.fusionize.workflow;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.component.*;
import dev.fusionize.workflow.component.runtime.DecisionComponentRuntime;
import dev.fusionize.workflow.component.runtime.EndComponentRuntime;
import dev.fusionize.workflow.component.runtime.StartComponentRuntime;
import dev.fusionize.workflow.component.runtime.TaskComponentRuntime;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * -----------------------------
 * SPRING TEST CONFIGURATION
 * -----------------------------
 * This configuration defines a simple in-memory EventPublisher
 * for testing workflow orchestration without needing messaging infrastructure.
 */
@Configuration
@ComponentScan(basePackages = "dev.fusionize.workflow")
@Import({
        TestMongoConfig.class,
        TestMongoConversionConfig.class
})
class TestConfig {

    static final class WorkflowApplicationEvent extends ApplicationEvent {
        public final Event event;
        public WorkflowApplicationEvent(Object source, Event event) {
            super(source);
            this.event = event;
        }
    }
    /**
     * Simple adapter to use Spring's ApplicationEventPublisher
     * as the workflow's EventPublisher abstraction.
     */
    @Bean
    public EventPublisher<Event> eventPublisher(ApplicationEventPublisher eventPublisher, EventStore<Event> eventStore) {
        return new EventPublisher<>(eventStore) {
            @Override
            public void publish(Event event) {
                super.publish(event);
                eventPublisher.publishEvent(new WorkflowApplicationEvent(event.getSource(), event));
            }
        };
    }

    @Bean
    public EventListener<Event> eventListener() {
        return new EventListener<Event>() {
            final List<EventCallback<Event>> callbacks = new ArrayList<>();
            @Override
            public void addListener(EventCallback<Event> callback) {
                callbacks.add(callback);
            }

            @org.springframework.context.event.EventListener()
            public void onEvent(WorkflowApplicationEvent workflowApplicationEvent){
                callbacks.forEach(c -> c.onEvent(workflowApplicationEvent.event));

            }
        };
    }
}


/**
 * -----------------------------
 * ORCHESTRATOR TEST
 * -----------------------------
 * This JUnit test defines a mock email workflow:
 *
 *  Start → Decision → (SendEmail → End)
 *
 * Two email routes exist: route1 and route2.
 * The test simulates incoming emails being processed in sequence
 * by a long-running asynchronous workflow.
 */
@DataMongoTest()
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = "dev.fusionize.workflow")
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles("ut")
class OrchestratorTest {
    public static Logger logger = LoggerFactory.getLogger(OrchestratorTest.class);

    @Autowired WorkflowComponentRegistry componentRegistry;
    @Autowired Orchestrator service;
    @Autowired WorkflowComponentRuntimeEngine runtimeEngine;
    @Autowired EventPublisher<Event> eventPublisher;
    @Autowired WorkflowRegistry workflowRegistry;

    @Test
    void orchestrate() throws InterruptedException, IOException {
        // Collect logs for debugging workflow behavior
        StringWriter writer = new StringWriter();

        // Inbox shared across async threads (thread-safe list)
        List<String> inbox = new CopyOnWriteArrayList<>();

        // Define factories for workflow runtime components
        WorkflowComponentFactory emailRecStartFactory = () -> new MockRecEmailComponentRuntime(writer, eventPublisher, inbox);
        WorkflowComponentFactory emailSendTaskFactory = () -> new MockSendEmailComponent(writer, eventPublisher);
        WorkflowComponentFactory emailDecisionFactory = () -> new MockEmailDecisionComponent(writer, eventPublisher);
        WorkflowComponentFactory emailEndStepFactory = () -> new MockEndEmailComponent(writer, eventPublisher);

        /**
         * Register all mock components with the registry.
         * Each component represents a runtime handler for a specific workflow node type.
         */
        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("receivedIncomingEmail")
                        .withCompatible(WorkflowNodeType.START)
                        .build(), emailRecStartFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("emailDecision")
                        .withCompatible(WorkflowNodeType.DECISION)
                        .build(), emailDecisionFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("sendEmail")
                        .withCompatible(WorkflowNodeType.TASK)
                        .build(), emailSendTaskFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("endEmailWorkflow")
                        .withCompatible(WorkflowNodeType.END)
                        .build(), emailEndStepFactory);

        URL yamlUrl = this.getClass().getResource("/email-workflow.yml");
        assertNotNull(yamlUrl);
        String yml = Files.readString(new File(yamlUrl.getFile()).toPath());

        Workflow workflow = new WorkflowDescriptor().fromYamlDescription(yml);
        workflow = workflowRegistry.register(workflow);
        // Start orchestrating the workflow
        service.orchestrate(workflow.getWorkflowId());

        // Simulate asynchronous email arrivals
        Thread.sleep(200);
        String worklog = "Receiving First Email";
        writer.append(worklog).append("\n");
        logger.info(worklog);
        inbox.add("test email route 1");

        Thread.sleep(500);
        worklog = "Receiving Second Email";
        writer.append(worklog).append("\n");
        logger.info(worklog);
        inbox.add("test email route 2");

        Thread.sleep(1000);
        String output = writer.toString();
        logger.info("writer out ->\n {}",output);
        String actual = "MockRecEmailComponentRuntime activated\n" +
                "Receiving First Email\n" +
                "MockRecEmailComponentRuntime handle email: test email route 1\n" +
                "MockSendEmailDecisionComponent activated\n" +
                "Decision made to route email: {outgoing1=true, outgoing2=false}\n" +
                "MockSendEmailComponent activated\n" +
                "sending email to outgoing1@email.com\n" +
                "BODY: test email route 1\n" +
                "MockEndEmailComponent activated\n" +
                "Receiving Second Email\n" +
                "MockRecEmailComponentRuntime handle email: test email route 2\n" +
                "MockSendEmailDecisionComponent activated\n" +
                "Decision made to route email: {outgoing1=false, outgoing2=true}\n" +
                "MockSendEmailComponent activated\n" +
                "sending email to outgoing2@email.com\n" +
                "BODY: test email route 2\n" +
                "MockEndEmailComponent activated\n" +
                "ComponentFinishedEvent finished\n" +
                "ComponentFinishedEvent finished\n";
        assertEquals(actual, output);
    }

    // --------------------------------------------------------------------------
    // MOCK COMPONENTS
    // --------------------------------------------------------------------------

    /**
     * Simulates the END component of the workflow.
     * Logs its activation and publishes completion asynchronously.
     */
    static final class MockEndEmailComponent extends EndComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockEndEmailComponent.class);
        private final StringWriter writer;

        MockEndEmailComponent(StringWriter writer, EventPublisher<Event> eventPublisher) {
            super(eventPublisher);
            this.writer = writer;
        }

        @Override
        public void configure(WorkflowComponentConfig config) {}

        @Override
        public void canActivate(ComponentActivatedEvent onActivate) {
            writer.append("MockEndEmailComponent activated\n");
            logger.info("MockEndEmailComponent activated");
            onActivate.setException(null);
            publish(onActivate);
        }

        @Override
        public void finish(ComponentFinishedEvent onFinish) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(700);
                    writer.append("ComponentFinishedEvent finished\n");
                    logger.info("ComponentFinishedEvent finished");
                    publish(onFinish);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error(throwable.getMessage(), throwable);
                }
            });
        }
    }

    /**
     * Decision component that chooses between two email routes
     * based on the email content.
     */
    static final class MockEmailDecisionComponent extends DecisionComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockEmailDecisionComponent.class);
        private final StringWriter writer;
        private Map<String, String> routeMap = new HashMap<>();

        MockEmailDecisionComponent(StringWriter writer, EventPublisher<Event> eventPublisher) {
            super(eventPublisher);
            this.writer = writer;
        }

        @Override
        public void decide(ComponentFinishedEvent onDecision) {
            WorkflowDecision decision = onDecision.getContext().getDecisions().getLast();
            String outgoingMessage = (String) onDecision.getContext().getData().get("email_message");

            // Determine which route(s) to follow based on message content
            List<String> messageRoute = routeMap.keySet().stream()
                    .filter(outgoingMessage::contains)
                    .map(k -> routeMap.get(k))
                    .toList();

            // Update decision options accordingly
            for (String nodeOption : decision.getOptionNodes().keySet()) {
                decision.getOptionNodes().put(nodeOption, messageRoute.contains(nodeOption));
            }

            writer.append("Decision made to route email: ").append(decision.getOptionNodes().toString()).append("\n");
            logger.info("Decision made: {}", decision.getOptionNodes());
            publish(onDecision);
        }

        @Override
        public void configure(WorkflowComponentConfig config) {
            if (config.getConfig().containsKey("routeMap")) {
                routeMap = (Map<String, String>) config.getConfig().get("routeMap");
            }
        }

        @Override
        public void canActivate(ComponentActivatedEvent onActivate) {
            String worklog;
            if (!routeMap.isEmpty() && !onActivate.getContext().getDecisions().isEmpty()) {
                worklog = "MockSendEmailDecisionComponent activated";
                onActivate.setException(null);
            } else {
                worklog = "MockSendEmailDecisionComponent not activated";
                onActivate.setException(new Exception("No email to send"));
            }
            writer.append(worklog).append("\n");
            logger.info(worklog);
            publish(onActivate);
        }
    }

    /**
     * Task component that sends emails.
     * Reads the target address from config and logs the email body.
     */
    static final class MockSendEmailComponent extends TaskComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockSendEmailComponent.class);
        private final StringWriter writer;
        private String address;

        MockSendEmailComponent(StringWriter writer, EventPublisher<Event> eventPublisher) {
            super(eventPublisher);
            this.writer = writer;
        }

        @Override
        public void configure(WorkflowComponentConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public void canActivate(ComponentActivatedEvent onActivate) {
            boolean hasMessage = onActivate.getContext().getData().containsKey("email_message");
            String worklog = hasMessage ? "MockSendEmailComponent activated" : "MockSendEmailComponent not activated";
            writer.append(worklog).append("\n");
            logger.info(worklog);

            onActivate.setException(hasMessage ? null : new Exception("No email to send"));
            publish(onActivate);
        }

        @Override
        public void run(ComponentFinishedEvent onFinish) {
            writer.append("sending email to ").append(address).append("\n");
            logger.info("sending email to {}", address);

            writer.append("BODY: ").append(onFinish.getContext().getData().get("email_message").toString()).append("\n");
            logger.info("BODY: {}", onFinish.getContext().getData().get("email_message"));

            publish(onFinish);
        }
    }

    /**
     * Start component that continuously polls an inbox list for new emails.
     * When an email arrives, it triggers the workflow chain.
     */
    static final class MockRecEmailComponentRuntime extends StartComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockRecEmailComponentRuntime.class);
        private final StringWriter writer;
        private final List<String> inbox;
        private String address;

        MockRecEmailComponentRuntime(StringWriter writer, EventPublisher<Event> eventPublisher, List<String> inbox) {
            super(eventPublisher);
            this.writer = writer;
            this.inbox = inbox;
        }

        @Override
        public void configure(WorkflowComponentConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public void canActivate(ComponentActivatedEvent onActivate) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);
                    writer.append("MockRecEmailComponentRuntime activated\n");
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
                            writer.append(worklog).append("\n");
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
