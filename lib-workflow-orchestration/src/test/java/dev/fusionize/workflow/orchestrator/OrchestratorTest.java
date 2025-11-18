package dev.fusionize.workflow.orchestrator;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.context.WorkflowDecision;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentBundle;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static Logger logger = LoggerFactory.getLogger(TestConfig.class);

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

    @Bean
    public List<LocalComponentBundle<? extends LocalComponentRuntime>> bundles(){
        return List.of(
                new LocalComponentBundle<LocalComponentRuntime>(
                        ()-> new LocalComponentRuntime() {
                            int delay;
                            @Override
                            public void configure(ComponentRuntimeConfig config) {
                                delay = config.getConfig().containsKey("delay") ? Integer.parseInt(
                                        config.getConfig().get("delay").toString()
                                ):  5 * 1000;
                            }

                            @Override
                            public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
                                emitter.success(workflowContext);
                            }

                            @Override
                            public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
                                try {
                                    logger.info("Waiting for {} seconds", delay);
                                    Thread.sleep(delay);
                                    workflowContext.getData().put("delayFromWait", delay);
                                    emitter.success(workflowContext);
                                } catch (InterruptedException e) {
                                    emitter.failure(e);
                                }

                            }
                        },
                        "Wait4FewSeconds"
                )
        );
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

    @Autowired
    ComponentRuntimeRegistry componentRegistry;
    @Autowired Orchestrator service;
    @Autowired
    ComponentRuntimeEngine componentRuntimeEngine;
    @Autowired EventPublisher<Event> eventPublisher;
    @Autowired
    WorkflowRepoRegistry workflowRegistry;

    @Test
    void orchestrate() throws InterruptedException, IOException {
        // Collect logs for debugging workflow behavior
        StringWriter writer = new StringWriter();

        // Inbox shared across async threads (thread-safe list)
        List<String> inbox = new CopyOnWriteArrayList<>();

        // Define factories for workflow runtime components
        ComponentRuntimeFactory<MockRecEmailComponentRuntime> emailRecStartFactory =
                () -> new MockRecEmailComponentRuntime(writer, inbox);
        ComponentRuntimeFactory<MockSendEmailComponent> emailSendTaskFactory =
                () -> new MockSendEmailComponent(writer);
        ComponentRuntimeFactory<MockEmailDecisionComponent> emailDecisionFactory =
                () -> new MockEmailDecisionComponent(writer);
        ComponentRuntimeFactory<MockEndEmailComponent> emailEndStepFactory =
                () -> new MockEndEmailComponent(writer);

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
        String expected = "MockRecEmailComponentRuntime activated\n" +
                "Receiving First Email\n" +
                "MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com\n" +
                "MockSendEmailDecisionComponent activated\n" +
                "Decision made to route email: {outgoing1=true, outgoing2=false}\n" +
                "MockSendEmailComponent activated\n" +
                "sending email to outgoing1@email.com\n" +
                "BODY: test email route 1\n" +
                "Receiving Second Email\n" +
                "MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com\n" +
                "MockSendEmailDecisionComponent activated\n" +
                "MockEndEmailComponent activated\n" +
                "Decision made to route email: {outgoing1=false, outgoing2=true}\n" +
                "MockSendEmailComponent activated\n" +
                "sending email to outgoing2@email.com\n" +
                "BODY: test email route 2\n" +
                "ComponentFinishedEvent finished after 500\n" +
                "MockEndEmailComponent activated\n" +
                "ComponentFinishedEvent finished after 500\n";
        assertEquals(expected, output);
    }

    // --------------------------------------------------------------------------
    // MOCK COMPONENTS
    // --------------------------------------------------------------------------

    /**
     * Simulates the END component of the workflow.
     * Logs its activation and publishes completion asynchronously.
     */
    static final class MockEndEmailComponent implements ComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockEndEmailComponent.class);
        private final StringWriter writer;

        MockEndEmailComponent(StringWriter writer) {
            this.writer = writer;
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {}

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            writer.append("MockEndEmailComponent activated\n");
            logger.info("MockEndEmailComponent activated");
            emitter.success(workflowContext);
        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            try {
                Thread.sleep(200);
                String delayFromLocalComponent = workflowContext.getData().get("delayFromWait").toString();
                writer.append("ComponentFinishedEvent finished after ").append(delayFromLocalComponent).append("\n");
                logger.info("ComponentFinishedEvent finished after {}", delayFromLocalComponent);
                emitter.success(workflowContext);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }

    /**
     * Decision component that chooses between two email routes
     * based on the email content.
     */
    static final class MockEmailDecisionComponent implements ComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockEmailDecisionComponent.class);
        private final StringWriter writer;
        private Map<String, String> routeMap = new HashMap<>();

        MockEmailDecisionComponent(StringWriter writer) {
            this.writer = writer;
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            if (config.getConfig().containsKey("routeMap")) {
                routeMap = (Map<String, String>) config.getConfig().get("routeMap");
            }
        }

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            String worklog;
            if (!routeMap.isEmpty() && !workflowContext.getDecisions().isEmpty()) {
                worklog = "MockSendEmailDecisionComponent activated";
                writer.append(worklog).append("\n");
                logger.info(worklog);
                emitter.success(workflowContext);
            } else {
                worklog = "MockSendEmailDecisionComponent not activated";
                writer.append(worklog).append("\n");
                logger.info(worklog);
                emitter.failure(new Exception("No route map found"));
            }

        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            WorkflowDecision decision = workflowContext.getDecisions().getLast();
            String outgoingMessage = (String) workflowContext.getData().get("email_message");

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
            emitter.success(workflowContext);
        }


    }

    /**
     * Task component that sends emails.
     * Reads the target address from config and logs the email body.
     */
    static final class MockSendEmailComponent implements ComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockSendEmailComponent.class);
        private final StringWriter writer;
        private String address;

        MockSendEmailComponent(StringWriter writer) {
            this.writer = writer;
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            boolean hasMessage = workflowContext.getData().containsKey("email_message");
            String worklog = hasMessage ? "MockSendEmailComponent activated" : "MockSendEmailComponent not activated";
            writer.append(worklog).append("\n");
            logger.info(worklog);
            if(hasMessage){
                emitter.success(workflowContext);
            }else {
                emitter.failure(new Exception("No email to send"));
            }
        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            writer.append("sending email to ").append(address).append("\n");
            logger.info("sending email to {}", address);

            writer.append("BODY: ").append(workflowContext.getData().get("email_message").toString()).append("\n");
            logger.info("BODY: {}", workflowContext.getData().get("email_message"));

            emitter.success(workflowContext);
        }

    }

    /**
     * Start component that continuously polls an inbox list for new emails.
     * When an email arrives, it triggers the workflow chain.
     */
    static final class MockRecEmailComponentRuntime implements ComponentRuntime {
        private static final Logger logger = LoggerFactory.getLogger(MockRecEmailComponentRuntime.class);
        private final StringWriter writer;
        private final List<String> inbox;
        private String address;

        MockRecEmailComponentRuntime(StringWriter writer, List<String> inbox) {
            this.writer = writer;
            this.inbox = inbox;
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            try {
                Thread.sleep(100);
                writer.append("MockRecEmailComponentRuntime activated\n");
                logger.info("MockRecEmailComponentRuntime activated");
                emitter.success(workflowContext);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            while (true) {
                try {
                    Thread.sleep(100);
                    logger.info("inbox size: {}", inbox.size());

                    if (!inbox.isEmpty()) {
                        // Remove the first email and process it
                        String email = inbox.removeFirst();

                        String worklog = "MockRecEmailComponentRuntime handle email: " + email + " from " + address;
                        writer.append(worklog).append("\n");
                        logger.info(worklog);

                        workflowContext.getData().put("email_message", email);
                        emitter.success(workflowContext);
                    }

                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing email", e);
                }
            }
        }

    }
}
