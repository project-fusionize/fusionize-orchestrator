package dev.fusionize.worker;

import dev.fusionize.worker.component.RuntimeComponentRegistrar;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.worker.oidc.OidcTokenClient;
import dev.fusionize.worker.stomp.WorkerStompSessionHandler;
import dev.fusionize.worker.workflow.WorkflowLoader;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowComponentRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass(Worker.class)
@EnableConfigurationProperties(WorkerProperties.class)
@ComponentScan(basePackages = {"dev.fusionize.workflow","dev.fusionize.worker"})
public class WorkerAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(WorkerAutoConfiguration.class);

    private final WorkerProperties workerProperties;

    public WorkerAutoConfiguration(WorkerProperties workerProperties) {
        this.workerProperties = workerProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public Worker worker() {
        return new Worker();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketStompClient stompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkerStompSessionHandler workerStompSessionHandler() {
        return new WorkerStompSessionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OidcTokenClient oidcTokenClient() {
        return new OidcTokenClient(workerProperties.getOrchestratorUrl());
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeComponentRegistrar runtimeComponentRegistrar(WorkflowComponentRepoRegistry registry) {
        return new RuntimeComponentRegistrar(registry);
    }

    static final class WorkflowApplicationEvent extends ApplicationEvent {
        public final Event event;
        public WorkflowApplicationEvent(Object source, Event event) {
            super(source);
            this.event = event;
        }
    }

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
    @Order(1)
    public ApplicationRunner connectStompClient(OidcTokenClient oidcTokenClient,
                                                WebSocketStompClient stompClient,
                                                StompSessionHandler sessionHandler) {
        return args -> {
            String url = WorkerProperties.toWebSocketUrl(workerProperties.getOrchestratorUrl()) + "/ws/1.0";
            logger.info("Connecting to WebSocket STOMP server at {}", url);
            try {
                String clientToken = oidcTokenClient.getClientCredentialsToken(
                        workerProperties.getOidcClientId(), workerProperties.getOidcClientSecret(), null, null
                );
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken);
                stompClient.connectAsync(url, headers, sessionHandler).get();
                logger.info("Connected successfully");
            } catch (Exception e) {
                logger.error("Failed to connect to WebSocket STOMP server", e);
            }
        };
    }

    @Bean
    @Order(2)
    public ApplicationRunner registerWorkflowComponents(List<ComponentRuntimeFactory<?>> factories,
                                                        ComponentRuntimeRegistry componentRegistry,
                                                        RuntimeComponentRegistrar registrar) {
        return args ->
            factories.forEach(f-> {
                boolean validFactory = registrar.isValidComponentFactory(f.getClass());
                RuntimeComponentDefinition definition = f.getClass().getAnnotation(RuntimeComponentDefinition.class);
                boolean validAnnotation = registrar.isValidComponentDefinition(definition);
                if(validFactory && validAnnotation) {
                    WorkflowComponent component = registrar.registerComponent(definition);
                    componentRegistry.registerFactory(component, f);
                    logger.info("Registered Factory: {} {}", f.getClass().getSimpleName(), component.getComponentId());
                }
            });
    }

    @Bean
    @Order(3)
    public ApplicationRunner registerWorkflows(Orchestrator service,
                                               WorkflowRegistry workflowRegistry) {
        return args -> {
            new WorkflowLoader().loadWorkflows(workerProperties.getWorkflowDefinitionsRoot()).forEach(workflow -> {
                workflow = workflowRegistry.register(workflow);
                logger.info("service.orchestrate {}",workflow.getWorkflowId());
                service.orchestrate(workflow.getWorkflowId());
            });

        };
    }

}
