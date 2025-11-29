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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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
 * <p>
 * Start → Decision → (SendEmail → End)
 * <p>
 * Two email routes exist: route1 and route2.
 * The test simulates incoming emails being processed in sequence
 * by a long-running asynchronous workflow.
 */
@DataMongoTest()
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = "dev.fusionize.workflow")
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles("ut")
@Execution(ExecutionMode.SAME_THREAD)
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

        private Workflow loadWorkflow(String path) throws IOException {
                URL yamlUrl = this.getClass().getResource(path);
                assertNotNull(yamlUrl);
                String yml = Files.readString(new File(yamlUrl.getFile()).toPath());

                Workflow workflow = new WorkflowDescriptor().fromYamlDescription(yml);
                workflow = workflowRegistry.register(workflow);
                // Start orchestrating the workflow
                service.orchestrate(workflow.getWorkflowId());
                return workflow;
        }

        private void logWorkflowLogs(String prefix, List<WorkflowLog> logs) {
                logger.info(prefix, logs.stream().map(WorkflowLog::toString)
                                .collect(Collectors.joining("\n")));
        }

        @Test
        void orchestrateWithScript() throws InterruptedException, IOException {
                Workflow workflow = loadWorkflow("/email-workflow-with-script.yml");
                Map<String, String> nodeKeyToId = getNodeKeyToIdMap(workflow);

                // Simulate asynchronous email arrivals
                Thread.sleep(200);
                inbox.add("invoice help needed.");

                Thread.sleep(500);
                inbox.add("submitted the invoice pricing.");

                waitForWorkflowCompletion(2, 15);

                List<WorkflowLog> logs = workflowLogRepository.findAll();
                logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));
                logWorkflowLogs("DB logs orchestrateWithScript ->\n{}", logs);

                List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
                assertEquals(3, workflowExecutions.size());
                List<WorkflowExecution> done = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
                List<WorkflowExecution> idles = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

                assertEquals(1, idles.size());
                assertEquals(2, done.size());

                List<WorkflowLog> idleLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(idles.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> firsRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(done.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> lastRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId()))
                                .toList();

                logWorkflowLogs("DB idleLogs ->\n{}", idleLogs);
                logWorkflowLogs("DB firsRunLogs ->\n{}", firsRunLogs);
                logWorkflowLogs("DB lastRunLogs ->\n{}", lastRunLogs);

                List<String> expectedMessages = List.of(
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: invoice help needed. from incoming@fusionize.dev",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: submitted the invoice pricing. from incoming@fusionize.dev");

                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions (Billing + Support)
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("extractFields"), List.of(
                                "script: Script ran successfully {email_message=invoice help needed., isBilling=true, isSupport=true, isSales=false}"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("forkRoute"), List.of(
                                "fork: Evaluation results: {billingRoute=true, salesRoute=false, supportRoute=true}"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("billingRoute"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "task:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("supportRoute"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to support-team@fusionize.dev",
                                "task:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("billingWait"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("supportWait"), List.of(
                                "delay: sleeping 180 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("joinAll"), List.of(
                                "join: Wait condition not yet met. Awaited: [billingWait, supportWait, salesWait], Found: [billingWait], Mode: THRESHOLD"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 180"));

                // Order assertions
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("extractFields"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("extractFields"), nodeKeyToId.get("forkRoute"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("forkRoute"), nodeKeyToId.get("billingRoute"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("forkRoute"), nodeKeyToId.get("supportRoute"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("billingRoute"), nodeKeyToId.get("billingWait"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("supportRoute"), nodeKeyToId.get("supportWait"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("joinAll"), nodeKeyToId.get("end"));

                // Last Run Assertions (Billing + Sales)
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("extractFields"), List.of(
                                "script: Script ran successfully {email_message=submitted the invoice pricing., isBilling=true, isSupport=false, isSales=true}"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("forkRoute"), List.of(
                                "fork: Evaluation results: {billingRoute=true, salesRoute=true, supportRoute=false}"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("billingRoute"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "task:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("salesRoute"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to sales-team@fusionize.dev",
                                "task:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("billingWait"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("salesWait"), List.of(
                                "delay: sleeping 300 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("joinAll"), List.of(
                                "join: Wait condition not yet met. Awaited: [billingWait, supportWait, salesWait], Found: [billingWait], Mode: THRESHOLD"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                // Order assertions
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("extractFields"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("extractFields"), nodeKeyToId.get("forkRoute"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("forkRoute"), nodeKeyToId.get("billingRoute"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("forkRoute"), nodeKeyToId.get("salesRoute"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("billingRoute"), nodeKeyToId.get("billingWait"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("salesRoute"), nodeKeyToId.get("salesWait"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("joinAll"), nodeKeyToId.get("end"));
        }

        private Map<String, String> getNodeKeyToIdMap(Workflow workflow) {
                Map<String, String> map = new HashMap<>();
                Set<String> visited = new HashSet<>();
                List<WorkflowNode> queue = new ArrayList<>(workflow.getNodes());

                while (!queue.isEmpty()) {
                        WorkflowNode node = queue.remove(0);
                        if (visited.contains(node.getWorkflowNodeId())) {
                                continue;
                        }
                        visited.add(node.getWorkflowNodeId());

                        if (node.getWorkflowNodeKey() != null) {
                                map.put(node.getWorkflowNodeKey(), node.getWorkflowNodeId());
                        }
                        if (node.getChildren() != null) {
                                queue.addAll(node.getChildren());
                        }
                }
                return map;
        }

        private void assertNodeLogs(List<WorkflowLog> logs, String nodeId, List<String> expectedMessages) {
                List<String> actualMessages = logs.stream()
                                .filter(l -> nodeId.equals(l.getWorkflowNodeId()))
                                .map(l -> l.getComponent() + ": " + l.getMessage())
                                .toList();

                assertEquals(expectedMessages.size(), actualMessages.size(), "Log count mismatch for node " + nodeId);
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = actualMessages.get(i);
                        assertTrue(actual.endsWith(expected),
                                        "Node " + nodeId + ": Expected log at index " + i + " to end with: " + expected
                                                        + "\nActual: " + actual);
                }
        }

        private void assertNodeOrder(List<WorkflowLog> logs, String earlierNodeId, String laterNodeId) {
                int maxEarlierIndex = -1;
                int minLaterIndex = Integer.MAX_VALUE;

                for (int i = 0; i < logs.size(); i++) {
                        String nodeId = logs.get(i).getWorkflowNodeId();
                        if (earlierNodeId.equals(nodeId)) {
                                maxEarlierIndex = i;
                        }
                        if (laterNodeId.equals(nodeId)) {
                                if (minLaterIndex == Integer.MAX_VALUE) {
                                        minLaterIndex = i;
                                }
                        }
                }

                if (maxEarlierIndex != -1 && minLaterIndex != Integer.MAX_VALUE) {
                        assertTrue(maxEarlierIndex < minLaterIndex,
                                        "Expected node " + earlierNodeId + " to finish before node " + laterNodeId
                                                        + " starts.");
                }
        }

        private void waitForWorkflowCompletion(int expectedDoneCount, int timeoutSeconds) throws InterruptedException {
                long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
                while (System.currentTimeMillis() < endTime) {
                        List<WorkflowExecution> executions = workflowExecutionRepository.findAll();
                        long doneCount = executions.stream()
                                        .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS)
                                        .count();
                        logger.info("waitForWorkflowCompletion {} / {} done", doneCount, executions.size());
                        if (doneCount >= expectedDoneCount) {
                                logger.info("waitForWorkflowCompletion -> done");
                                return;
                        }
                        Thread.sleep(1000);
                }
                logger.info("waitForWorkflowCompletion time out {}", timeoutSeconds);

        }

        @Test
        void orchestrateWithForkJs() throws InterruptedException, IOException {
                loadWorkflow("/email-workflow-with-fork-js.yml");

                // Simulate asynchronous email arrivals
                Thread.sleep(200);
                inbox.add("test email route 1");

                Thread.sleep(500);
                inbox.add("test email route 2");

                waitForWorkflowCompletion(2, 15);

                List<WorkflowLog> logs = workflowLogRepository.findAll();
                logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));
                logWorkflowLogs("DB logs orchestrateWithForkJs ->\n{}", logs);

                List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
                assertEquals(3, workflowExecutions.size());
                List<WorkflowExecution> done = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
                List<WorkflowExecution> idles = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

                assertEquals(1, idles.size());
                assertEquals(2, done.size());

                List<WorkflowLog> idleLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(idles.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> firsRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(done.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> lastRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId()))
                                .toList();

                logWorkflowLogs("DB idleLogs ->\n{}", idleLogs);
                logWorkflowLogs("DB firsRunLogs ->\n{}", firsRunLogs);
                logWorkflowLogs("DB lastRunLogs ->\n{}", lastRunLogs);

                List<String> expectedMessages = List.of(
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                expectedMessages = List.of(
                                "fork: Evaluation results: {outgoing1=true, outgoing2=false}",
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: test email route 1",
                                "delay: sleeping 500 milliseconds",
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
                assertEquals(expectedMessages.size(), firsRunLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = firsRunLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                expectedMessages = List.of(
                                "fork: Evaluation results: {outgoing1=false, outgoing2=true}",
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: test email route 2",
                                "delay: sleeping 500 milliseconds",
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
                assertEquals(expectedMessages.size(), lastRunLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = lastRunLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }
        }

        @Test
        void orchestrateWithFork() throws InterruptedException, IOException {
                loadWorkflow("/email-workflow-with-fork.yml");

                // Simulate asynchronous email arrivals
                Thread.sleep(200);
                inbox.add("test email route 1");

                Thread.sleep(500);
                inbox.add("test email route 2");

                waitForWorkflowCompletion(2, 15);

                List<WorkflowLog> logs = workflowLogRepository.findAll();
                logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));
                logWorkflowLogs("DB logs orchestrateWithFork ->\n{}", logs);

                List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
                assertEquals(3, workflowExecutions.size());
                List<WorkflowExecution> done = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
                List<WorkflowExecution> idles = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

                assertEquals(1, idles.size());
                assertEquals(2, done.size());

                List<WorkflowLog> idleLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(idles.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> firsRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(done.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> lastRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId()))
                                .toList();

                logWorkflowLogs("DB idleLogs ->\n{}", idleLogs);
                logWorkflowLogs("DB firsRunLogs ->\n{}", firsRunLogs);
                logWorkflowLogs("DB lastRunLogs ->\n{}", lastRunLogs);

                List<String> expectedMessages = List.of(
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                expectedMessages = List.of(
                                "fork: Evaluation results: {outgoing1=true, outgoing2=false}",
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: test email route 1",
                                "delay: sleeping 500 milliseconds",
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
                assertEquals(expectedMessages.size(), firsRunLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = firsRunLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                expectedMessages = List.of(
                                "fork: Evaluation results: {outgoing1=false, outgoing2=true}",
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: test email route 2",
                                "delay: sleeping 500 milliseconds",
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
                assertEquals(expectedMessages.size(), lastRunLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = lastRunLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }
        }

        @Test
        void orchestrateParallelPickLast() throws InterruptedException, IOException {
                Workflow workflow = loadWorkflow("/email-workflow-parallel-pick-last.yml");
                Map<String, String> nodeKeyToId = getNodeKeyToIdMap(workflow);

                // Simulate asynchronous email arrivals
                Thread.sleep(200);
                inbox.add("First Email Content");

                Thread.sleep(500);
                inbox.add("Second Email Content");

                waitForWorkflowCompletion(2, 15);

                List<WorkflowLog> logs = workflowLogRepository.findAll();
                logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));
                logWorkflowLogs("DB logs orchestrateParallelPickLast ->\n{}", logs);

                List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
                assertEquals(3, workflowExecutions.size());
                List<WorkflowExecution> done = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
                List<WorkflowExecution> idles = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

                assertEquals(1, idles.size());
                assertEquals(2, done.size());

                List<WorkflowLog> idleLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(idles.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> firsRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(done.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> lastRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId()))
                                .toList();

                logWorkflowLogs("DB idleLogs ->\n{}", idleLogs);
                logWorkflowLogs("DB firsRunLogs ->\n{}", firsRunLogs);
                logWorkflowLogs("DB lastRunLogs ->\n{}", lastRunLogs);

                List<String> expectedMessages = List.of(
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing1"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing2"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing3"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing3@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting1"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting2"), List.of(
                                "delay: sleeping 230 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting3"), List.of(
                                "delay: sleeping 300 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waitFor123"), List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing1"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing2"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing3"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing1"), nodeKeyToId.get("waiting1"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing2"), nodeKeyToId.get("waiting2"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing3"), nodeKeyToId.get("waiting3"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("waitFor123"), nodeKeyToId.get("end"));

                // Last Run Assertions
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing1"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing2"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing3"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing3@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting1"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting2"), List.of(
                                "delay: sleeping 230 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting3"), List.of(
                                "delay: sleeping 300 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waitFor123"), List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing1"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing2"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing3"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing1"), nodeKeyToId.get("waiting1"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing2"), nodeKeyToId.get("waiting2"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing3"), nodeKeyToId.get("waiting3"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("waitFor123"), nodeKeyToId.get("end"));
        }

        @Test
        void orchestrateParallelPickFirst() throws InterruptedException, IOException {
                Workflow workflow = loadWorkflow("/email-workflow-parallel-pick-first.yml");
                Map<String, String> nodeKeyToId = getNodeKeyToIdMap(workflow);

                // Simulate asynchronous email arrivals
                Thread.sleep(200);
                inbox.add("First Email Content");

                Thread.sleep(500);
                inbox.add("Second Email Content");

                waitForWorkflowCompletion(2, 15);

                List<WorkflowLog> logs = workflowLogRepository.findAll();
                logs.sort(Comparator.comparing(WorkflowLog::getTimestamp));
                logWorkflowLogs("DB logs orchestrateParallelPickFirst ->\n{}", logs);
                List<WorkflowExecution> workflowExecutions = workflowExecutionRepository.findAll();
                assertEquals(3, workflowExecutions.size());
                List<WorkflowExecution> done = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.SUCCESS).toList();
                List<WorkflowExecution> idles = workflowExecutions.stream()
                                .filter(we -> we.getStatus() == WorkflowExecutionStatus.IDLE).toList();

                assertEquals(1, idles.size());
                assertEquals(2, done.size());

                List<WorkflowLog> idleLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(idles.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> firsRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId()
                                                .equals(done.getFirst().getWorkflowExecutionId()))
                                .toList();
                List<WorkflowLog> lastRunLogs = logs.stream()
                                .filter(l -> l.getWorkflowExecutionId().equals(done.getLast().getWorkflowExecutionId()))
                                .toList();

                logWorkflowLogs("DB idleLogs ->\n{}", idleLogs);
                logWorkflowLogs("DB firsRunLogs ->\n{}", firsRunLogs);
                logWorkflowLogs("DB lastRunLogs ->\n{}", lastRunLogs);

                List<String> expectedMessages = List.of(
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                                "start:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing1"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing2"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("outgoing3"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing3@email.com",
                                "task:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting1"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting2"), List.of(
                                "delay: sleeping 230 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waiting3"), List.of(
                                "delay: sleeping 300 milliseconds"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("waitFor123"), List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(firsRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 120"));

                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing1"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing2"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing3"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing1"), nodeKeyToId.get("waiting1"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing2"), nodeKeyToId.get("waiting2"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("outgoing3"), nodeKeyToId.get("waiting3"));
                assertNodeOrder(firsRunLogs, nodeKeyToId.get("waitFor123"), nodeKeyToId.get("end"));

                // Last Run Assertions
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing1"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing1@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing2"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing2@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("outgoing3"), List.of(
                                "task:test.sendEmail: MockSendEmailComponent activated",
                                "task:test.sendEmail: sending email to outgoing3@email.com",
                                "task:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting1"), List.of(
                                "delay: sleeping 120 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting2"), List.of(
                                "delay: sleeping 230 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waiting3"), List.of(
                                "delay: sleeping 300 milliseconds"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("waitFor123"), List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(lastRunLogs, nodeKeyToId.get("end"), List.of(
                                "end:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "end:test.endEmailWorkflow: ComponentFinishedEvent finished after 120"));

                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing1"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing2"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("start"), nodeKeyToId.get("outgoing3"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing1"), nodeKeyToId.get("waiting1"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing2"), nodeKeyToId.get("waiting2"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("outgoing3"), nodeKeyToId.get("waiting3"));
                assertNodeOrder(lastRunLogs, nodeKeyToId.get("waitFor123"), nodeKeyToId.get("end"));
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
                                Optional<Integer> delayFromLocal = context.var(DelayComponent.VAR_DELAYED,
                                                Integer.class);
                                emitter.logger().info("ComponentFinishedEvent finished after {}",
                                                delayFromLocal.orElse(-1));
                                emitter.success(context);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                        }
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
                                                emitter.logger().info(
                                                                "MockRecEmailComponentRuntime handle email: {} from {}",
                                                                email, address);
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
