package dev.fusionize.workflow;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.component.*;

import dev.fusionize.workflow.component.runtime.ComponentEvent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeStart;
import dev.fusionize.workflow.component.runtime.event.ComponentActivateEventData;
import dev.fusionize.workflow.component.runtime.event.ComponentTriggeredEventData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TestMongoConfig.class,
        TestMongoConversionConfig.class,
        WorkflowComponentRuntimeEngine.class,
        WorkflowComponentRegistry.class,
        WorkflowOrchestrationService.class
})
@ActiveProfiles("ut")
class WorkflowOrchestrationServiceTest {
    @Autowired
    WorkflowComponentRegistry registry;

    @Autowired
    WorkflowOrchestrationService service;

    @Autowired
    WorkflowComponentRuntimeEngine runtimeEngine;

    @Autowired
    ApplicationEventPublisher eventPublisher;


    @Test
    void orchestrate() throws InterruptedException {
        StringWriter writer = new StringWriter();
        MockRecEmailComponent mockRecEmailComponent = new MockRecEmailComponent(writer, eventPublisher);
        WorkflowComponentFactory emailTaskFactory = () -> mockRecEmailComponent;

        registry.registerFactory(WorkflowComponent.builder("test")
                .withDomain("receivedEmail")
                .withCompatible(WorkflowNodeType.START)
                .build(), emailTaskFactory);
        Workflow workflow = Workflow.builder("test")
                .addNode(WorkflowNode.builder()
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

//    static final class MockSendEmailComponent implements ComponentRuntimeTask {
//        private final StringWriter writer;
//        String address;
//
//        MockSendEmailComponent(StringWriter writer) {
//            this.writer = writer;
//        }
//
//        @Override
//        public void run(WorkflowContext context, Predicate<WorkflowContext> onFinish) {
//            System.out.println("sending email to " + address);
//            System.out.println("BODY: " + context.getContext().get("outgoing_message"));
//        }
//
//        @Override
//        public void configure(WorkflowComponentConfig config) {
//            this.address = config.getConfig().get("address").toString();
//        }
//
//        @Override
//        public boolean canActivate(WorkflowContext context) {
//            return true;
//        }
//
//        @Override
//        public ComponentRuntime clone() throws CloneNotSupportedException {
//            return (ComponentRuntime) super.clone();
//        }
//    }

    static final class MockRecEmailComponent implements ComponentRuntimeStart {
        private final StringWriter writer;
        private final ApplicationEventPublisher eventPublisher;

        String address;
        private List<String> inbox = new ArrayList<>();

        MockRecEmailComponent(StringWriter writer, ApplicationEventPublisher eventPublisher) {
            this.writer = writer;
            this.eventPublisher = eventPublisher;
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
        public void canActivate(ComponentEvent<ComponentActivateEventData> onActivate) {
            try {
                Thread.sleep(100);
                onActivate.getData().setActivated(true);
                eventPublisher.publishEvent(onActivate);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());

            }
        }

        @Override
        public void start(ComponentEvent<ComponentTriggeredEventData> onTriggered) {
            CompletableFuture.runAsync(()->{
                while (true){
                    try {
                        Thread.sleep(100);
                        WorkflowContext ctx = onTriggered.getData().getContext();
                        for(String email : inbox){
                            ctx.getContext().put("incoming_message", email);
                            eventPublisher.publishEvent(onTriggered);
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