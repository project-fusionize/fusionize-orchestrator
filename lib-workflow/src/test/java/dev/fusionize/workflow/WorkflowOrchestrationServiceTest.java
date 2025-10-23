package dev.fusionize.workflow;

import dev.fusionize.workflow.component.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

class WorkflowOrchestrationServiceTest {
    WorkflowOrchestrationService service;
    WorkflowComponentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WorkflowComponentRegistry();
        service = new WorkflowOrchestrationService(registry);
    }

    @Test
    void orchestrate() throws InterruptedException {
        StringWriter writer = new StringWriter();
        MockRecEmailComponent mockRecEmailComponent = new MockRecEmailComponent(writer);
        WorkflowComponentFactory emailTaskFactory = () -> mockRecEmailComponent;

        registry.registerFactory(WorkflowComponent.builder("test")
                .withDomain("receivedEmail")
                .withCompatible(WorkflowNodeType.START)
                .build(), emailTaskFactory);
        Workflow workflow = Workflow.builder("test")
                .addNode(WorkflowNode.builder()
                        .workflowNodeId("node-1")
                        .component("start:test.receivedEmail")
                        .componentConfig(WorkflowComponentConfig.builder().put("address", "a@b.com").build())
                        .type(WorkflowNodeType.START)
                ).build();
        service.orchestrate(workflow);
        Thread.sleep(2000);
        mockRecEmailComponent.addToInbox("test email");
        Thread.sleep(2000);
        mockRecEmailComponent.addToInbox("test email 2");
        Thread.sleep(2000);
        System.out.println(writer.toString());
    }

    static final class MockSendEmailComponent implements WorkflowComponentRuntimeTask {
        private final StringWriter writer;
        String address;

        MockSendEmailComponent(StringWriter writer) {
            this.writer = writer;
        }

        @Override
        public void run(WorkflowContext context, Predicate<WorkflowContext> onFinish) {
            System.out.println("sending email to " + address);
            System.out.println("BODY: " + context.getContext().get("outgoing_message"));
        }

        @Override
        public void configure(WorkflowComponentConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public boolean canActivate(WorkflowContext context) {
            return true;
        }

        @Override
        public WorkflowComponentRuntime clone() throws CloneNotSupportedException {
            return (WorkflowComponentRuntime) super.clone();
        }
    }

    static final class MockRecEmailComponent implements WorkflowComponentRuntimeStart {
        private final StringWriter writer;

        String address;
        private List<String> inbox = new ArrayList<>();

        MockRecEmailComponent(StringWriter writer) {
            this.writer = writer;
        }

        public void addToInbox(String email) {
            this.writer.append("incoming email to: ").append(address).append("\n");
            System.out.println("incoming email to: "+address);
            this.inbox.add(email);
        }


        @Override
        public void configure(WorkflowComponentConfig config) {
            this.address = config.getConfig().get("address").toString();
        }

        @Override
        public boolean canActivate(WorkflowContext context) {
            return true;
        }

        @Override
        public WorkflowComponentRuntime clone() throws CloneNotSupportedException {
            return (WorkflowComponentRuntime) super.clone();
        }

        @Override
        public void start(Predicate<WorkflowContext> onTriggered) {
            CompletableFuture.runAsync(()->{
                while (true){
                    try {
                        Thread.sleep(100);
                        for(String email : inbox){
                            WorkflowContext ctx = new WorkflowContext();
                            ctx.getContext().put("incoming_message", email);
                            onTriggered.test(ctx);
                        }
                        inbox.clear();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());

                    }
                }

            });
        }
    }
}