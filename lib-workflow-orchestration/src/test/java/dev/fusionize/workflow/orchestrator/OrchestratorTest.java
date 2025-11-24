package dev.fusionize.workflow.orchestrator;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.*;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.beans.DelayComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowDecision;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.logging.WorkflowLogRepository;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
import dev.fusionize.workflow.repo.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
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
    WorkflowRepository workflowRepository;
    @Autowired
    WorkflowLogRepository workflowLogRepository;
    @Autowired
    WorkflowExecutionRepository workflowExecutionRepository;
    List<String> inbox;


    @BeforeEach
    public void setUp() throws IOException {
        workflowRepository.deleteAll();
        workflowExecutionRepository.deleteAll();

        // Inbox shared across async threads (thread-safe list)
        inbox = new CopyOnWriteArrayList<>();

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

    }

    private void loadWorkflow(String path) throws IOException {
        URL yamlUrl = this.getClass().getResource(path);
        assertNotNull(yamlUrl);
        String yml = Files.readString(new File(yamlUrl.getFile()).toPath());

        Workflow workflow = new WorkflowDescriptor().fromYamlDescription(yml);
        workflow = workflowRegistry.register(workflow);
        // Start orchestrating the workflow
        service.orchestrate(workflow.getWorkflowId());
    }

    @Test
    void orchestrate() throws InterruptedException, IOException {
        loadWorkflow("/email-workflow-with-fork.yml");

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
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
        List<WorkflowExecution> idles = workflowExecutions.stream()
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

        assertEquals(1, idles.size());
        assertEquals(2, done.size());

        List<WorkflowLog> idleLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(idles.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> firsRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> lastRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId())).toList();

        logger.info("DB idleLogs ->\n{}", idleLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB firsRunLogs ->\n{}", firsRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB lastRunLogs ->\n{}", lastRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));

        List<String> expectedMessages = List.of(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com"
        );
        assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = idleLogs.get(i).toString();
            assertTrue(actual.endsWith(expected),
                    "Expected log at index " + i + " to end with: " + expected + "\nActual: " + actual);
        }

        expectedMessages = List.of(
                "decision:test.emailDecision: MockSendEmailDecisionComponent activated",
                "decision:test.emailDecision: Decision made to route email: {outgoing1=true, outgoing2=false}",
                "task:test.sendEmail: MockSendEmailComponent activated",
                "task:test.sendEmail: sending email to outgoing1@email.com",
                "task:test.sendEmail: BODY: test email route 1",
                "delay: sleeping 500 milliseconds",
                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500"
        );
        assertEquals(expectedMessages.size(), firsRunLogs.size(), "Log count mismatch");
        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = firsRunLogs.get(i).toString();
            assertTrue(actual.endsWith(expected),
                    "Expected log at index " + i + " to end with: " + expected + "\nActual: " + actual);
        }

        expectedMessages = List.of(
                "decision:test.emailDecision: MockSendEmailDecisionComponent activated",
                "decision:test.emailDecision: Decision made to route email: {outgoing1=false, outgoing2=true}",
                "task:test.sendEmail: MockSendEmailComponent activated",
                "task:test.sendEmail: sending email to outgoing2@email.com",
                "task:test.sendEmail: BODY: test email route 2",
                "delay: sleeping 500 milliseconds",
                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500"
        );
        assertEquals(expectedMessages.size(), lastRunLogs.size(), "Log count mismatch");
        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = lastRunLogs.get(i).toString();
            assertTrue(actual.endsWith(expected),
                    "Expected log at index " + i + " to end with: " + expected + "\nActual: " + actual);
        }
    }

    @Test
    void orchestrateParallelPickLast() throws InterruptedException, IOException {
        loadWorkflow("/email-workflow-parallel-pick-last.yml");
        // Simulate asynchronous email arrivals
        Thread.sleep(200);
        String worklog = "Receiving First Email";
        logger.info(worklog);
        inbox.add("First Email Content");

        Thread.sleep(500);
        worklog = "Receiving Second Email";
        logger.info(worklog);
        inbox.add("Second Email Content");

        Thread.sleep(2000);

        List<WorkflowLog> logs = workflowLogRepository.findAll();
        logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));

        List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
        logger.info("DB logs ->\n{}", logs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        assertEquals(3, workflowExecutions.size());
        List<WorkflowExecution> done = workflowExecutions.stream()
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
        List<WorkflowExecution> idles = workflowExecutions.stream()
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

        assertEquals(1, idles.size());
        assertEquals(2, done.size());

        List<WorkflowLog> idleLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(idles.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> firsRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> lastRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId())).toList();

        logger.info("DB idleLogs ->\n{}", idleLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB firsRunLogs ->\n{}", firsRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB lastRunLogs ->\n{}", lastRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));

        List<String> expectedMessages = List.of(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com"
        );
        assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = idleLogs.get(i).toString();
            assertTrue(actual.endsWith(expected),
                    "Expected log at index " + i + " to end with: " + expected + "\nActual: " + actual);
        }

        Map<String, Integer> expectedMessagesMap = Map.of(
                "task:test.sendEmail: MockSendEmailComponent activated", 3,
                "task:test.sendEmail: sending email to outgoing1@email.com", 1,
                "task:test.sendEmail: sending email to outgoing2@email.com", 1,
                "task:test.sendEmail: sending email to outgoing3@email.com", 1,
                "task:test.sendEmail: BODY: First Email Content",3,
                "end:test.endEmailWorkflow: MockEndEmailComponent activated", 1,
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 300", 1
        );
        for (String key : expectedMessagesMap.keySet()) {
            long expected = expectedMessagesMap.get(key);
            long actual = firsRunLogs.stream().filter(m->m.toString().endsWith(key)).count();
            assertEquals(expected, actual,
                    "Expected log line" + key + " to appear: " + expected + "(times) Actual: " + actual);
        }

        expectedMessagesMap = Map.of(
                "task:test.sendEmail: MockSendEmailComponent activated", 3,
                "task:test.sendEmail: sending email to outgoing1@email.com", 1,
                "task:test.sendEmail: sending email to outgoing2@email.com", 1,
                "task:test.sendEmail: sending email to outgoing3@email.com", 1,
                "task:test.sendEmail: BODY: Second Email Content",3,
                "end:test.endEmailWorkflow: MockEndEmailComponent activated", 1,
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 300", 1
        );
        for (String key : expectedMessagesMap.keySet()) {
            long expected = expectedMessagesMap.get(key);
            long actual = lastRunLogs.stream().filter(m->m.toString().endsWith(key)).count();
            assertEquals(expected, actual,
                    "Expected log line" + key + " to appear: " + expected + "(times) Actual: " + actual);
        }
    }

    @Test
    void orchestrateParallelPickFirst() throws InterruptedException, IOException {
        loadWorkflow("/email-workflow-parallel-pick-first.yml");
        // Simulate asynchronous email arrivals
        Thread.sleep(200);
        String worklog = "Receiving First Email";
        logger.info(worklog);
        inbox.add("First Email Content");

        Thread.sleep(500);
        worklog = "Receiving Second Email";
        logger.info(worklog);
        inbox.add("Second Email Content");

        Thread.sleep(2000);

        List<WorkflowLog> logs = workflowLogRepository.findAll();
        logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));

        List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
        logger.info("DB logs ->\n{}", logs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        assertEquals(3, workflowExecutions.size());
        List<WorkflowExecution> done = workflowExecutions.stream()
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
        List<WorkflowExecution> idles = workflowExecutions.stream()
                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

        assertEquals(1, idles.size());
        assertEquals(2, done.size());

        List<WorkflowLog> idleLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(idles.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> firsRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getFirst().getWorkflowExecutionId())).toList();
        List<WorkflowLog> lastRunLogs = logs.stream()
                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId())).toList();

        logger.info("DB idleLogs ->\n{}", idleLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB firsRunLogs ->\n{}", firsRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));
        logger.info("DB lastRunLogs ->\n{}", lastRunLogs.stream().map(WorkflowLog::toString)
                .collect(Collectors.joining("\n")));

        List<String> expectedMessages = List.of(
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com"
        );
        assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
        for (int i = 0; i < expectedMessages.size(); i++) {
            String expected = expectedMessages.get(i);
            String actual = idleLogs.get(i).toString();
            assertTrue(actual.endsWith(expected),
                    "Expected log at index " + i + " to end with: " + expected + "\nActual: " + actual);
        }

        Map<String, Integer> expectedMessagesMap = Map.of(
                "task:test.sendEmail: MockSendEmailComponent activated", 3,
                "task:test.sendEmail: sending email to outgoing1@email.com", 1,
                "task:test.sendEmail: sending email to outgoing2@email.com", 1,
                "task:test.sendEmail: sending email to outgoing3@email.com", 1,
                "task:test.sendEmail: BODY: First Email Content",3,
                "end:test.endEmailWorkflow: MockEndEmailComponent activated", 1,
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 120", 1
        );
        for (String key : expectedMessagesMap.keySet()) {
            long expected = expectedMessagesMap.get(key);
            long actual = firsRunLogs.stream().filter(m->m.toString().endsWith(key)).count();
            assertEquals(expected, actual,
                    "Expected log line" + key + " to appear: " + expected + "(times) Actual: " + actual);
        }

        expectedMessagesMap = Map.of(
                "task:test.sendEmail: MockSendEmailComponent activated", 3,
                "task:test.sendEmail: sending email to outgoing1@email.com", 1,
                "task:test.sendEmail: sending email to outgoing2@email.com", 1,
                "task:test.sendEmail: sending email to outgoing3@email.com", 1,
                "task:test.sendEmail: BODY: Second Email Content",3,
                "end:test.endEmailWorkflow: MockEndEmailComponent activated", 1,
                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 120", 1
        );
        for (String key : expectedMessagesMap.keySet()) {
            long expected = expectedMessagesMap.get(key);
            long actual = lastRunLogs.stream().filter(m->m.toString().endsWith(key)).count();
            assertEquals(expected, actual,
                    "Expected log line" + key + " to appear: " + expected + "(times) Actual: " + actual);
        }
    }

    // --------------------------------------------------------------------------
    // MOCK COMPONENTS
    // --------------------------------------------------------------------------

    /**
     * Simulates the END component of the workflow.
     * Logs its activation and publishes completion asynchronously.
     */
    static final class MockEndEmailComponent implements ComponentRuntime {

        MockEndEmailComponent() {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
        }

        @Override
        public void canActivate(Context context, ComponentUpdateEmitter emitter) {
            emitter.logger().info("MockEndEmailComponent activated");
            emitter.success(context);
        }

        @Override
        public void run(Context context, ComponentUpdateEmitter emitter) {
            try {
                Thread.sleep(200);
                Optional<Integer> delayFromLocal = context.var(DelayComponent.VAR_DELAYED, Integer.class);
                emitter.logger().info("ComponentFinishedEvent finished after {}", delayFromLocal.orElse(-1));
                emitter.success(context);
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
        private Map<String, String> routeMap = new HashMap<>();

        MockEmailDecisionComponent() {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            if (config.contains("routeMap")) {
                routeMap = config.varMap("routeMap").orElse(new HashMap<>());
            }
        }

        @Override
        public void canActivate(Context context, ComponentUpdateEmitter emitter) {
            if (!routeMap.isEmpty() && !context.getDecisions().isEmpty()) {
                emitter.logger().info("MockSendEmailDecisionComponent activated");
                emitter.success(context);
            } else {
                emitter.logger().info("MockSendEmailDecisionComponent not activated");
                emitter.failure(new Exception("No route map found"));
            }

        }

        @Override
        public void run(Context context, ComponentUpdateEmitter emitter) {
            WorkflowDecision decision = context.getDecisions().getLast();
            String outgoingMessage = context.varString("email_message").orElse("");

            // Determine which route(s) to follow based on message content
            List<String> messageRoute = routeMap.keySet().stream()
                    .filter(outgoingMessage::contains)
                    .map(k -> routeMap.get(k))
                    .toList();

            // Update decision options accordingly
            for (String nodeOption : decision.getOptionNodes().keySet()) {
                decision.getOptionNodes().put(nodeOption, messageRoute.contains(nodeOption));
            }

            emitter.logger().info("Decision made to route email: {}", decision.getOptionNodes().toString());
            emitter.success(context);
        }
    }

    /**
     * Task component that sends emails.
     * Reads the target address from config and logs the email body.
     */
    static final class MockSendEmailComponent implements ComponentRuntime {
        private String address;

        MockSendEmailComponent() {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            this.address = config.varString("address").orElse(null);
        }

        @Override
        public void canActivate(Context context, ComponentUpdateEmitter emitter) {
            boolean hasMessage = context.contains("email_message");
            if (hasMessage) {
                emitter.logger().info("MockSendEmailComponent activated");
                emitter.success(context);
            } else {
                emitter.logger().info("MockSendEmailComponent not activated");
                emitter.failure(new Exception("No email to send"));
            }
        }

        @Override
        public void run(Context context, ComponentUpdateEmitter emitter) {
            try {
                emitter.logger().info("sending email to {}", address);
                Thread.sleep(10);
                emitter.logger().info("BODY: {}", context.varString("email_message").orElse(""));
                emitter.success(context);
            } catch (InterruptedException e) {
                emitter.failure(e);
            }
        }

    }

    /**
     * Start component that continuously polls an inbox list for new emails.
     * When an email arrives, it triggers the workflow chain.
     */
    static final class MockRecEmailComponentRuntime implements ComponentRuntime {
        private final List<String> inbox;
        private String address;

        MockRecEmailComponentRuntime(List<String> inbox) {
            this.inbox = inbox;
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
            this.address = config.varString("address").orElse(null);
        }

        @Override
        public void canActivate(Context context, ComponentUpdateEmitter emitter) {
            try {
                Thread.sleep(100);
                emitter.logger().info("MockRecEmailComponentRuntime activated");
                emitter.success(context);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run(Context context, ComponentUpdateEmitter emitter) {
            while (true) {
                try {
                    Thread.sleep(100);
                    logger.info("inbox size: {}", inbox.size());

                    if (!inbox.isEmpty()) {
                        // Remove the first email and process it
                        String email = inbox.removeFirst();
                        emitter.logger().info("MockRecEmailComponentRuntime handle email: {} from {}", email, address);
                        context.set("email_message", email);
                        emitter.success(context);
                    }

                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    emitter.logger().error("Error processing email", e);
                    emitter.failure(e);
                    logger.error("Error processing email", e);
                }
            }
        }

    }
}
