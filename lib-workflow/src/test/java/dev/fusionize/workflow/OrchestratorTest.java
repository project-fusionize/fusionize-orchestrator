package dev.fusionize.workflow;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.component.*;

import dev.fusionize.workflow.component.runtime.StartComponentRuntime;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.events.RuntimeEvent;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentTriggeredEvent;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Configuration
@ComponentScan(basePackages = "dev.fusionize.workflow")
@Import({
        TestMongoConfig.class,
        TestMongoConversionConfig.class
})
class TestConfig {
    @Bean
    public EventStore<Event> eventStore(){
        return new EventStore<Event>() {
            Map<String, Event> map = new HashMap<>();
            @Override
            public void save(Event event) {
                map.put(event.getEventId(),event);
            }

            @Override
            public Optional<Event> findByEventId(String eventId) {
                return Optional.ofNullable(map.get(eventId));
            }

            @Override
            public List<Event> findByCausationId(String causationId) {
                return map.values().stream().filter(e-> causationId.equals(e.getCausationId())).toList();
            }

            @Override
            public List<Event> findByCorrelationId(String correlationId) {
                return map.values().stream().filter(e-> correlationId.equals(e.getCorrelationId())).toList();
            }
        };
    }

    @Bean
    public EventPublisher<Event> eventPublisher(ApplicationEventPublisher eventPublisher) {
        return eventPublisher::publishEvent;
    }
}


@DataMongoTest()
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = "dev.fusionize.workflow")
@ContextConfiguration(
        classes = TestConfig.class)
@ActiveProfiles("ut")
class OrchestratorTest {
    @Autowired
    WorkflowComponentRegistry registry;

    @Autowired
    Orchestrator service;

    @Autowired
    WorkflowComponentRuntimeEngine runtimeEngine;

    @Autowired
    EventPublisher<Event> eventPublisher;


    @Test
    void orchestrate() throws InterruptedException {
        StringWriter writer = new StringWriter();
        MockRecEmailComponentComponentRuntime mockRecEmailComponent = new MockRecEmailComponentComponentRuntime(writer, eventPublisher);
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

    static final class MockRecEmailComponentComponentRuntime extends StartComponentRuntime {
        private final StringWriter writer;

        String address;
        private List<String> inbox = new ArrayList<>();

        MockRecEmailComponentComponentRuntime(StringWriter writer, EventPublisher<Event> eventPublisher) {
            super(eventPublisher);
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
        public void canActivate(ComponentActivatedEvent onActivate) {
            CompletableFuture.runAsync(()->{
                try {
                    Thread.sleep(100);
                    onActivate.setException(null);
                    publish(onActivate);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());

                }
            });

        }

        @Override
        public void start(ComponentTriggeredEvent onTriggered) {
            CompletableFuture.runAsync(()->{
                while (true){
                    try {
                        Thread.sleep(100);
                        WorkflowContext ctx = onTriggered.getContext();
                        for(String email : inbox){
                            ctx.getContext().put("incoming_message", email);
                            publish(onTriggered);
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