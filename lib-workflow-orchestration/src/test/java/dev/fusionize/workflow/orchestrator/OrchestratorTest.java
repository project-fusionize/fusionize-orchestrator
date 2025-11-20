package dev.fusionize.workflow.orchestrator;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.*;
import dev.fusionize.workflow.context.WorkflowContext;
import dev.fusionize.workflow.context.WorkflowDecision;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentBundle;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.logging.WorkflowLogRepository;
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
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
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
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
    public EventPublisher<Event> eventPublisher(ApplicationEventPublisher eventPublisher,
            EventStore<Event> eventStore) {
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
            public void onEvent(WorkflowApplicationEvent workflowApplicationEvent) {
                callbacks.forEach(c -> c.onEvent(workflowApplicationEvent.event));

            }
        };
    }

    @Bean
    public List<LocalComponentBundle<? extends LocalComponentRuntime>> bundles() {
        return List.of(
                new LocalComponentBundle<LocalComponentRuntime>(
                        () -> new LocalComponentRuntime() {
                            int delay;

                            @Override
                            public void configure(ComponentRuntimeConfig config) {
                                delay = config.getConfig().containsKey("delay") ? Integer.parseInt(
                                        config.getConfig().get("delay").toString()) : 5 * 1000;
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
                        "Wait4FewSeconds"));
    }
}

/**
 * -----------------------------
 * ORCHESTRATOR TEST
 * -----------------------------
 * This JUnit test defines a mock email workflow:
 *
 * Start → Decision → (SendEmail → End)
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
    @Autowired
    Orchestrator service;
    @Autowired
    ComponentRuntimeEngine componentRuntimeEngine;
    @Autowired
    EventPublisher<Event> eventPublisher;
    @Autowired
    WorkflowRepoRegistry workflowRegistry;

    @Autowired
    WorkflowLogRepository workflowLogRepository;
    @Autowired
    WorkflowExecutionRepository workflowExecutionRepository;

    @Test
    void orchestrate() throws InterruptedException, IOException {
        // Inbox shared across async threads (thread-safe list)
        List<String> inbox = new CopyOnWriteArrayList<>();

        // Define factories for workflow runtime components
        ComponentRuntimeFactory<MockRecEmailComponentRuntime> emailRecStartFactory = () -> new MockRecEmailComponentRuntime(
                inbox);
        ComponentRuntimeFactory<MockSendEmailComponent> emailSendTaskFactory = MockSendEmailComponent::new;
        ComponentRuntimeFactory<MockEmailDecisionComponent> emailDecisionFactory = MockEmailDecisionComponent::new;
        ComponentRuntimeFactory<MockEndEmailComponent> emailEndStepFactory = MockEndEmailComponent::new;

        /**
         * Register all mock components with the registry.
         * Each component represents a runtime handler for a specific workflow node
         * type.
         */
        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("receivedIncomingEmail")
                        .withCompatible(WorkflowNodeType.START)
                        .build(),
                emailRecStartFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("emailDecision")
                        .withCompatible(WorkflowNodeType.DECISION)
                        .build(),
                emailDecisionFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("sendEmail")
                        .withCompatible(WorkflowNodeType.TASK)
                        .build(),
                emailSendTaskFactory);

        componentRegistry.registerFactory(
                WorkflowComponent.builder("test")
                        .withDomain("endEmailWorkflow")
                        .withCompatible(WorkflowNodeType.END)
                        .build(),
                emailEndStepFactory);

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
        logger.info(worklog);
        inbox.add("test email route 1");

        Thread.sleep(500);
        worklog = "Receiving Second Email";
        logger.info(worklog);
        inbox.add("test email route 2");

        Thread.sleep(2000);


        List<WorkflowLog> logs = workflowLogRepository.findAll();
        logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));

        List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
        logger.info("DB logs ->\n{}", logs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        assertEquals(3, workflowExecutions.size());
        List<WorkflowExecution> done = workflowExecutions.stream()
                .filter(we->we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
        List<WorkflowExecution> idles = workflowExecutions.stream()
                .filter(we->we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

        assertEquals(1, idles.size());
        assertEquals(2, done.size());

        List<WorkflowLog> idleLogs = logs.stream().filter(l->
                l.getWorkflowExecutionId().equals(idles.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> firsRunLogs = logs.stream().filter(l->
                l.getWorkflowExecutionId().equals(done.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> lastRunLogs = logs.stream().filter(l->
                l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId())).toList();

        logger.info("DB idleLogs ->\n{}", idleLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB firsRunLogs ->\n{}", firsRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB lastRunLogs ->\n{}", lastRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));

        assertEquals(3, idleLogs.size());
        assertTrue(idleLogs.get(0).toString().endsWith(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated"));
        assertTrue(idleLogs.get(1).toString().endsWith(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com"));
        assertTrue(idleLogs.get(2).toString().endsWith(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com"));

        assertEquals(7, firsRunLogs.size());
        assertTrue(firsRunLogs.get(0).toString().endsWith("decision:test.emailDecision: MockSendEmailDecisionComponent activated"));
        assertTrue(firsRunLogs.get(1).toString().endsWith("decision:test.emailDecision: Decision made to route email: {outgoing1=true, outgoing2=false}"));
        assertTrue(firsRunLogs.get(2).toString().endsWith("task:test.sendEmail: MockSendEmailComponent activated"));
        assertTrue(firsRunLogs.get(3).toString().endsWith("task:test.sendEmail: sending email to outgoing1@email.com"));
        assertTrue(firsRunLogs.get(4).toString().endsWith("task:test.sendEmail: BODY: test email route 1"));
        assertTrue(firsRunLogs.get(5).toString().endsWith("end:test.endEmailWorkflow: MockEndEmailComponent activated"));
        assertTrue(firsRunLogs.get(6).toString().endsWith("end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500"));

        assertEquals(7, lastRunLogs.size());
        assertTrue(lastRunLogs.get(0).toString().endsWith("decision:test.emailDecision: MockSendEmailDecisionComponent activated"));
        assertTrue(lastRunLogs.get(1).toString().endsWith("decision:test.emailDecision: Decision made to route email: {outgoing1=false, outgoing2=true}"));
        assertTrue(lastRunLogs.get(2).toString().endsWith("task:test.sendEmail: MockSendEmailComponent activated"));
        assertTrue(lastRunLogs.get(3).toString().endsWith("task:test.sendEmail: sending email to outgoing2@email.com"));
        assertTrue(lastRunLogs.get(4).toString().endsWith("task:test.sendEmail: BODY: test email route 2"));
        assertTrue(lastRunLogs.get(5).toString().endsWith("end:test.endEmailWorkflow: MockEndEmailComponent activated"));
        assertTrue(lastRunLogs.get(6).toString().endsWith("end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500"));
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

        MockEndEmailComponent() {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
        }

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            emitter.log("MockEndEmailComponent activated");
            logger.info("MockEndEmailComponent activated");
            emitter.success(workflowContext);
        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            try {
                Thread.sleep(200);
                String delayFromLocalComponent = workflowContext.getData().get("delayFromWait").toString();
                emitter.log("ComponentFinishedEvent finished after " + delayFromLocalComponent);
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
        private Map<String, String> routeMap = new HashMap<>();

        MockEmailDecisionComponent() {
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
                emitter.log(worklog);
                logger.info(worklog);
                emitter.success(workflowContext);
            } else {
                worklog = "MockSendEmailDecisionComponent not activated";
                emitter.log(worklog);
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

            emitter.log("Decision made to route email: " + decision.getOptionNodes().toString());
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
        private String address;

        MockSendEmailComponent() {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public void canActivate(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            boolean hasMessage = workflowContext.getData().containsKey("email_message");
            String worklog = hasMessage ? "MockSendEmailComponent activated" : "MockSendEmailComponent not activated";
            emitter.log(worklog);
            logger.info(worklog);
            if (hasMessage) {
                emitter.success(workflowContext);
            } else {
                emitter.failure(new Exception("No email to send"));
            }
        }

        @Override
        public void run(WorkflowContext workflowContext, ComponentUpdateEmitter emitter) {
            emitter.log("sending email to " + address);
            logger.info("sending email to {}", address);

            emitter.log("BODY: " + workflowContext.getData().get("email_message").toString());
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
        private final List<String> inbox;
        private String address;

        MockRecEmailComponentRuntime(List<String> inbox) {
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
                emitter.log("MockRecEmailComponentRuntime activated");
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
                        emitter.log(worklog);
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
