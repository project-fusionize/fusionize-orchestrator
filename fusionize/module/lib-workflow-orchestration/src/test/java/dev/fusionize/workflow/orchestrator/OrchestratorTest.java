package dev.fusionize.workflow.orchestrator;

import dev.fusionize.process.Process;
import dev.fusionize.process.ProcessConverter;
import dev.fusionize.storage.file.FileStorageService;
import dev.fusionize.storage.file.FileStorageServiceLocal;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.logging.WorkflowLogRepository;
import dev.fusionize.workflow.orchestrator.helpers.*;
import dev.fusionize.workflow.orchestrator.helpers.TestConfig;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        BlockingQueue<InputStream> fileQueue;

        @Autowired
        ProcessConverter processConverter;
        FileStorageService fileStorageService;

        @BeforeEach
        public void setUp() throws IOException {
                fileStorageService = new FileStorageServiceLocal("/tmp/fs");
                workflowRepository.deleteAll();
                workflowExecutionRepository.deleteAll();

                // Inbox shared across async threads (thread-safe list)
                inbox = new CopyOnWriteArrayList<>();
                fileQueue = new LinkedBlockingQueue<>();

                // Define factories for workflow runtime components
                ComponentRuntimeFactory<MockRecEmailComponentRuntime> emailRecStartFactory = () -> new MockRecEmailComponentRuntime(
                                inbox);
                ComponentRuntimeFactory<MockSendEmailComponent> emailSendTaskFactory = MockSendEmailComponent::new;
                ComponentRuntimeFactory<MockEndEmailComponent> emailEndStepFactory = MockEndEmailComponent::new;

                ComponentRuntimeFactory<MockDataExtractorComponent> fileExtractorFactory = () -> new MockDataExtractorComponent(
                                fileStorageService);
                ComponentRuntimeFactory<MockFileHandlerComponent> fileStartFactory = () -> new MockFileHandlerComponent(
                                fileStorageService, fileQueue);
                ComponentRuntimeFactory<MockEndFileComponent> fileEndFactory = MockEndFileComponent::new;

                /**
                 * Register all mock components with the registry.
                 * Each component represents a runtime handler for a specific workflow node
                 * type.
                 */
                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("receivedIncomingEmail")
                                                .withActor(Actor.SYSTEM)
                                                .build(),
                                emailRecStartFactory);

                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("sendEmail")
                                                .withActor(Actor.AI)
                                                .build(),
                                emailSendTaskFactory);

                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("endEmailWorkflow")
                                                .withActor(Actor.SYSTEM)
                                                .build(),
                                emailEndStepFactory);

                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("mockFileHandler")
                                                .withActor(Actor.SYSTEM)
                                                .build(),
                                fileStartFactory);

                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("mockDataExtractor")
                                                .withActor(Actor.AI)
                                                .build(),
                                fileExtractorFactory);

                componentRegistry.registerFactory(
                                WorkflowComponent.builder("test")
                                                .withDomain("endFileWorkflow")
                                                .withActor(Actor.SYSTEM)
                                                .build(),
                                fileEndFactory);

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
                loadWorkflow("/email-workflow-with-script.yml");

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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: invoice help needed. from incoming@fusionize.dev",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: submitted the invoice pricing. from incoming@fusionize.dev");

                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions (Billing + Support)
                assertNodeLogs(firsRunLogs, "extractFields", List.of(
                                "script: Script ran successfully {email_message=invoice help needed., isBilling=true, isSupport=true, isSales=false}"));
                assertNodeLogs(firsRunLogs, "forkRoute", List.of(
                                "fork: Evaluation results: {salesRoute=false, supportRoute=true, billingRoute=true}"));
                assertNodeLogs(firsRunLogs, "billingRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, "supportRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to support-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, "billingWait", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(firsRunLogs, "supportWait", List.of(
                                "delay: scheduling delay of 180 milliseconds"));
                assertNodeLogs(firsRunLogs, "joinAll", List.of(
                                "join: Wait condition not yet met. Awaited: [billingWait, supportWait, salesWait], Found: [billingWait], Mode: THRESHOLD"));
                assertNodeLogs(firsRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 180"));

                // Order assertions
                assertNodeOrder(firsRunLogs, "start", "extractFields");
                assertNodeOrder(firsRunLogs, "extractFields", "forkRoute");
                assertNodeOrder(firsRunLogs, "forkRoute", "billingRoute");
                assertNodeOrder(firsRunLogs, "forkRoute", "supportRoute");
                assertNodeOrder(firsRunLogs, "billingRoute", "billingWait");
                assertNodeOrder(firsRunLogs, "supportRoute", "supportWait");
                assertNodeOrder(firsRunLogs, "joinAll", "end");

                // Last Run Assertions (Billing + Sales)
                assertNodeLogs(lastRunLogs, "extractFields", List.of(
                                "script: Script ran successfully {email_message=submitted the invoice pricing., isBilling=true, isSupport=false, isSales=true}"));
                assertNodeLogs(lastRunLogs, "forkRoute", List.of(
                                "fork: Evaluation results: {salesRoute=true, supportRoute=false, billingRoute=true}"));
                assertNodeLogs(lastRunLogs, "billingRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, "salesRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to sales-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, "billingWait", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(lastRunLogs, "salesWait", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(lastRunLogs, "joinAll", List.of(
                                "join: Wait condition not yet met. Awaited: [billingWait, supportWait, salesWait], Found: [billingWait], Mode: THRESHOLD"));
                assertNodeLogs(lastRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                // Order assertions
                assertNodeOrder(lastRunLogs, "start", "extractFields");
                assertNodeOrder(lastRunLogs, "extractFields", "forkRoute");
                assertNodeOrder(lastRunLogs, "forkRoute", "billingRoute");
                assertNodeOrder(lastRunLogs, "forkRoute", "salesRoute");
                assertNodeOrder(lastRunLogs, "billingRoute", "billingWait");
                assertNodeOrder(lastRunLogs, "salesRoute", "salesWait");
                assertNodeOrder(lastRunLogs, "joinAll", "end");
        }

        private void assertNodeLogs(List<WorkflowLog> logs, String nodeKey, List<String> expectedMessages) {
                List<String> actualMessages = logs.stream()
                                .filter(l -> nodeKey.equals(l.getNodeKey()))
                                .map(l -> l.getComponent() + ": " + l.getMessage())
                                .toList();

                assertEquals(expectedMessages.size(), actualMessages.size(), "Log count mismatch for node " + nodeKey);
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = actualMessages.get(i);
                        assertTrue(actual.endsWith(expected),
                                        "Node " + nodeKey + ": Expected log at index " + i + " to end with: " + expected
                                                        + "\nActual: " + actual);
                }
        }

        private void assertNodeOrder(List<WorkflowLog> logs, String earlierNodeKey, String laterNodeKey) {
                int maxEarlierIndex = -1;
                int minLaterIndex = Integer.MAX_VALUE;

                for (int i = 0; i < logs.size(); i++) {
                        String nodeKey = logs.get(i).getNodeKey();
                        if (earlierNodeKey.equals(nodeKey)) {
                                maxEarlierIndex = i;
                        }
                        if (laterNodeKey.equals(nodeKey)) {
                                if (minLaterIndex == Integer.MAX_VALUE) {
                                        minLaterIndex = i;
                                }
                        }
                }

                if (maxEarlierIndex != -1 && minLaterIndex != Integer.MAX_VALUE) {
                        assertTrue(maxEarlierIndex < minLaterIndex,
                                        "Expected node " + earlierNodeKey + " to finish before node " + laterNodeKey
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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com");
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
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: test email route 1",
                                "delay: scheduling delay of 500 milliseconds",
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
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
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: test email route 2",
                                "delay: scheduling delay of 500 milliseconds",
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 1 from incoming@email.com",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: test email route 2 from incoming@email.com");
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
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: test email route 1",
                                "delay: scheduling delay of 500 milliseconds",
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
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
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: test email route 2",
                                "delay: scheduling delay of 500 milliseconds",
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 500");
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
                loadWorkflow("/email-workflow-parallel-pick-last.yml");

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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions
                assertNodeLogs(firsRunLogs, "outgoing1", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "outgoing2", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "outgoing3", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing3@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "waiting1", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(firsRunLogs, "waiting2", List.of(
                                "delay: scheduling delay of 230 milliseconds"));
                assertNodeLogs(firsRunLogs, "waiting3", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(firsRunLogs, "waitFor123", List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(firsRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                assertNodeOrder(firsRunLogs, "start", "outgoing1");
                assertNodeOrder(firsRunLogs, "start", "outgoing2");
                assertNodeOrder(firsRunLogs, "start", "outgoing3");
                assertNodeOrder(firsRunLogs, "outgoing1", "waiting1");
                assertNodeOrder(firsRunLogs, "outgoing2", "waiting2");
                assertNodeOrder(firsRunLogs, "outgoing3", "waiting3");
                assertNodeOrder(firsRunLogs, "waitFor123", "end");

                // Last Run Assertions
                assertNodeLogs(lastRunLogs, "outgoing1", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "outgoing2", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "outgoing3", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing3@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "waiting1", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(lastRunLogs, "waiting2", List.of(
                                "delay: scheduling delay of 230 milliseconds"));
                assertNodeLogs(lastRunLogs, "waiting3", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(lastRunLogs, "waitFor123", List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(lastRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                assertNodeOrder(lastRunLogs, "start", "outgoing1");
                assertNodeOrder(lastRunLogs, "start", "outgoing2");
                assertNodeOrder(lastRunLogs, "start", "outgoing3");
                assertNodeOrder(lastRunLogs, "outgoing1", "waiting1");
                assertNodeOrder(lastRunLogs, "outgoing2", "waiting2");
                assertNodeOrder(lastRunLogs, "outgoing3", "waiting3");
                assertNodeOrder(lastRunLogs, "waitFor123", "end");
        }

        @Test
        void orchestrateParallelPickFirst() throws InterruptedException, IOException {
                loadWorkflow("/email-workflow-parallel-pick-first.yml");

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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: First Email Content from incoming@email.com",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: Second Email Content from incoming@email.com");
                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions
                assertNodeLogs(firsRunLogs, "outgoing1", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "outgoing2", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "outgoing3", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing3@email.com",
                                "ai:test.sendEmail: BODY: First Email Content"));
                assertNodeLogs(firsRunLogs, "waiting1", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(firsRunLogs, "waiting2", List.of(
                                "delay: scheduling delay of 230 milliseconds"));
                assertNodeLogs(firsRunLogs, "waiting3", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(firsRunLogs, "waitFor123", List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(firsRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 120"));

                assertNodeOrder(firsRunLogs, "start", "outgoing1");
                assertNodeOrder(firsRunLogs, "start", "outgoing2");
                assertNodeOrder(firsRunLogs, "start", "outgoing3");
                assertNodeOrder(firsRunLogs, "outgoing1", "waiting1");
                assertNodeOrder(firsRunLogs, "outgoing2", "waiting2");
                assertNodeOrder(firsRunLogs, "outgoing3", "waiting3");
                assertNodeOrder(firsRunLogs, "waitFor123", "end");

                // Last Run Assertions
                assertNodeLogs(lastRunLogs, "outgoing1", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing1@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "outgoing2", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing2@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "outgoing3", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to outgoing3@email.com",
                                "ai:test.sendEmail: BODY: Second Email Content"));
                assertNodeLogs(lastRunLogs, "waiting1", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(lastRunLogs, "waiting2", List.of(
                                "delay: scheduling delay of 230 milliseconds"));
                assertNodeLogs(lastRunLogs, "waiting3", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(lastRunLogs, "waitFor123", List.of(
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1], Mode: ALL",
                                "join: Wait condition not yet met. Awaited: [waiting1, waiting2, waiting3], Found: [waiting1, waiting2], Mode: ALL"));
                assertNodeLogs(lastRunLogs, "end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 120"));

                assertNodeOrder(lastRunLogs, "start", "outgoing1");
                assertNodeOrder(lastRunLogs, "start", "outgoing2");
                assertNodeOrder(lastRunLogs, "start", "outgoing3");
                assertNodeOrder(lastRunLogs, "outgoing1", "waiting1");
                assertNodeOrder(lastRunLogs, "outgoing2", "waiting2");
                assertNodeOrder(lastRunLogs, "outgoing3", "waiting3");
                assertNodeOrder(lastRunLogs, "waitFor123", "end");
        }

        @Test
        void orchestrateProcessWithScript() throws InterruptedException, IOException, XMLStreamException {
                URL xmlUrl = this.getClass().getResource("/email-workflow-with-script.bpmn20.xml");
                URL ymlUrl = this.getClass().getResource("/email-workflow-with-script.bpmn20.yml");
                assertNotNull(ymlUrl);
                assertNotNull(xmlUrl);
                String xml = Files.readString(new File(xmlUrl.getFile()).toPath());
                String yml = Files.readString(new File(ymlUrl.getFile()).toPath());
                Process process = processConverter.convert(xml);
                processConverter.annotate(process, yml);

                Workflow workflow = processConverter.convertToWorkflow(process);
                workflow = workflowRegistry.register(workflow);
                System.out.println(new WorkflowDescriptor().toYamlDescription(workflow));
                // Start orchestrating the workflow
                service.orchestrate(workflow.getWorkflowId());

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
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime activated",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: invoice help needed. from incoming@fusionize.dev",
                                "system:test.receivedIncomingEmail: MockRecEmailComponentRuntime handle email: submitted the invoice pricing. from incoming@fusionize.dev");

                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions (Billing + Support)
                assertNodeLogs(firsRunLogs, "scriptTask#extractFields", List.of(
                                "script: Script ran successfully {email_message=invoice help needed., isBilling=true, isSupport=true, isSales=false}"));
                assertNodeLogs(firsRunLogs, "inclusiveGateway#forkRoute", List.of(
                                "fork: Evaluation results: {serviceTask#salesRoute=false, serviceTask#billingRoute=true, serviceTask#supportRoute=true}"));
                assertNodeLogs(firsRunLogs, "serviceTask#billingRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, "serviceTask#supportRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to support-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: invoice help needed."));
                assertNodeLogs(firsRunLogs, "intermediateCatchEvent#billingWait", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(firsRunLogs, "intermediateCatchEvent#supportWait", List.of(
                                "delay: scheduling delay of 180 milliseconds"));
                assertNodeLogs(firsRunLogs, "inclusiveGateway#joinAll", List.of(
                                "join: Wait condition not yet met. Awaited: [intermediateCatchEvent#billingWait, intermediateCatchEvent#supportWait, intermediateCatchEvent#salesWait], Found: [intermediateCatchEvent#billingWait], Mode: THRESHOLD"));
                assertNodeLogs(firsRunLogs, "endEvent#end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 180"));

                // Order assertions
                assertNodeOrder(firsRunLogs, "startEvent#startstart", "scriptTask#extractFields");
                assertNodeOrder(firsRunLogs, "scriptTask#extractFields", "inclusiveGateway#forkRoute");
                assertNodeOrder(firsRunLogs, "inclusiveGateway#forkRoute", "serviceTask#billingRoute");
                assertNodeOrder(firsRunLogs, "inclusiveGateway#forkRoute", "serviceTask#supportRoute");
                assertNodeOrder(firsRunLogs, "serviceTask#billingRoute", "intermediateCatchEvent#billingWait");
                assertNodeOrder(firsRunLogs, "serviceTask#supportRoute", "intermediateCatchEvent#supportWait");
                assertNodeOrder(firsRunLogs, "inclusiveGateway#joinAll", "endEvent#end");

                // Last Run Assertions (Billing + Sales)
                assertNodeLogs(lastRunLogs, "scriptTask#extractFields", List.of(
                                "script: Script ran successfully {email_message=submitted the invoice pricing., isBilling=true, isSupport=false, isSales=true}"));
                assertNodeLogs(lastRunLogs, "inclusiveGateway#forkRoute", List.of(
                                "fork: Evaluation results: {serviceTask#salesRoute=true, serviceTask#billingRoute=true, serviceTask#supportRoute=false}"));
                assertNodeLogs(lastRunLogs, "serviceTask#billingRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to billing-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, "serviceTask#salesRoute", List.of(
                                "ai:test.sendEmail: MockSendEmailComponent activated",
                                "ai:test.sendEmail: sending email to sales-team@fusionize.dev",
                                "ai:test.sendEmail: BODY: submitted the invoice pricing."));
                assertNodeLogs(lastRunLogs, "intermediateCatchEvent#billingWait", List.of(
                                "delay: scheduling delay of 120 milliseconds"));
                assertNodeLogs(lastRunLogs, "intermediateCatchEvent#salesWait", List.of(
                                "delay: scheduling delay of 300 milliseconds"));
                assertNodeLogs(lastRunLogs, "inclusiveGateway#joinAll", List.of(
                                "join: Wait condition not yet met. Awaited: [intermediateCatchEvent#billingWait, intermediateCatchEvent#supportWait, intermediateCatchEvent#salesWait], Found: [intermediateCatchEvent#billingWait], Mode: THRESHOLD"));
                assertNodeLogs(lastRunLogs, "endEvent#end", List.of(
                                "system:test.endEmailWorkflow: MockEndEmailComponent activated",
                                "system:test.endEmailWorkflow: ComponentFinishedEvent finished after 300"));

                // Order assertions
                assertNodeOrder(lastRunLogs, "startEvent#startstart", "scriptTask#extractFields");
                assertNodeOrder(lastRunLogs, "scriptTask#extractFields", "inclusiveGateway#forkRoute");
                assertNodeOrder(lastRunLogs, "inclusiveGateway#forkRoute", "serviceTask#billingRoute");
                assertNodeOrder(lastRunLogs, "inclusiveGateway#forkRoute", "serviceTask#salesRoute");
                assertNodeOrder(lastRunLogs, "serviceTask#billingRoute", "intermediateCatchEvent#billingWait");
                assertNodeOrder(lastRunLogs, "serviceTask#salesRoute", "intermediateCatchEvent#salesWait");
                assertNodeOrder(lastRunLogs, "inclusiveGateway#joinAll", "endEvent#end");
        }

        @Test
        void orchestrateWithFile() throws InterruptedException, IOException {
                loadWorkflow("/file-workflow-with-mock-extraction.yml");

                Thread.sleep(200);
                URL jsonUrl = this.getClass().getResource("/test/test1.json");
                assertNotNull(jsonUrl);
                InputStream is = jsonUrl.openStream();
                assertTrue(fileQueue.offer(is));

                Thread.sleep(500);
                jsonUrl = this.getClass().getResource("/test/test2.json");
                assertNotNull(jsonUrl);
                is = jsonUrl.openStream();
                assertTrue(fileQueue.offer(is));

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
                                "system:test.mockFileHandler: MockFileHandlerComponent activated",
                                "system:test.mockFileHandler: MockFileHandlerComponent handling file stream",
                                "system:test.mockFileHandler: MockFileHandlerComponent handling file stream");

                assertEquals(expectedMessages.size(), idleLogs.size(), "Log count mismatch");
                for (int i = 0; i < expectedMessages.size(); i++) {
                        String expected = expectedMessages.get(i);
                        String actual = idleLogs.get(i).toString();
                        assertTrue(actual.endsWith(expected),
                                        "Expected log at index " + i + " to end with: " + expected + "\nActual: "
                                                        + actual);
                }

                // First Run Assertions (123)
                assertNodeLogs(firsRunLogs, "extractData", List.of(
                                "ai:test.mockDataExtractor: MockDataExtractorComponent activated",
                                "ai:test.mockDataExtractor: Extracted: 123"));
                assertNodeLogs(firsRunLogs, "end", List.of(
                                "system:test.endFileWorkflow: MockEndFileComponent activated",
                                "system:test.endFileWorkflow: ComponentFinishedEvent finished with data 123"));

                // Order assertions
                assertNodeOrder(firsRunLogs, "start", "extractData");
                assertNodeOrder(firsRunLogs, "extractData", "end");

                // Last Run Assertions (456)
                assertNodeLogs(lastRunLogs, "extractData", List.of(
                                "ai:test.mockDataExtractor: MockDataExtractorComponent activated",
                                "ai:test.mockDataExtractor: Extracted: 456"));
                assertNodeLogs(lastRunLogs, "end", List.of(
                                "system:test.endFileWorkflow: MockEndFileComponent activated",
                                "system:test.endFileWorkflow: ComponentFinishedEvent finished with data 456"));

                // Order assertions
                assertNodeOrder(lastRunLogs, "start", "extractData");
                assertNodeOrder(lastRunLogs, "extractData", "end");
        }
}
