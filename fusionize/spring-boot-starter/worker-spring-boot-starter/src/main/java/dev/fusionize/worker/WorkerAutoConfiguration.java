package dev.fusionize.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.model.descriptor.AgentConfigDescriptor;
import dev.fusionize.ai.model.descriptor.ChatModelConfigDescriptor;
import dev.fusionize.ai.service.AgentConfigManager;
import dev.fusionize.ai.service.ChatModelManager;
import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.storage.StorageConfig;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.descriptor.StorageConfigDescriptor;
import dev.fusionize.worker.component.RuntimeComponentRegistrar;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.worker.oidc.OidcTokenClient;
import dev.fusionize.worker.stomp.WorkerStompSessionHandler;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.registry.WorkflowComponentRepoRegistry;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import dev.fusionize.workflow.orchestrator.Orchestrator;
import dev.fusionize.workflow.registry.WorkflowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnClass(Worker.class)
@EnableConfigurationProperties(WorkerProperties.class)
@ComponentScan(basePackages = { "dev.fusionize.workflow", "dev.fusionize.worker" })
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
    public WebSocketStompClient stompClient(ObjectMapper objectMapper) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
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
                        workerProperties.getOidcClientId(), workerProperties.getOidcClientSecret(), null, null);
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
        return args -> factories.forEach(f -> {
            boolean validFactory = registrar.isValidComponentFactory(f.getClass());
            RuntimeComponentDefinition definition = f.getClass().getAnnotation(RuntimeComponentDefinition.class);
            boolean validAnnotation = registrar.isValidComponentDefinition(definition);
            if (validFactory && validAnnotation) {
                WorkflowComponent component = registrar.registerComponent(definition);
                componentRegistry.registerFactory(component, f);
                logger.info("Registered Factory: {} {}", f.getClass().getSimpleName(), component.getComponentId());
            }else {
                logger.warn("Skip Factory registration: {} {}", f.getClass().getSimpleName(), validAnnotation
                                ? "name, description or meta information is missing"
                                : "is not implementing ComponentRuntimeFactory");
            }
        });
    }

    @Bean
    @Order(3)
    public ApplicationRunner registerResources(Orchestrator service,
                                               WorkflowRegistry workflowRegistry,
                                               AgentConfigManager agentConfigManager,
                                               ChatModelManager chatModelManager,
                                               StorageConfigManager storageConfigManager) {
        return args -> {
            File root = new File(workerProperties.getResourceRoot());
            if (!root.exists() || !root.isDirectory()) {
                logger.warn("Workflow definitions root does not exist or is not a directory: {}", root.getAbsolutePath());
                return;
            }

            Collection<File> files = org.apache.commons.io.FileUtils.listFiles(root, new String[]{"yml", "yaml"}, true);
            YamlParser<Map> parser = new YamlParser<>();

            for (File file : files) {
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    Map<String, Object> description = parser.fromYaml(content, Map.class);

                    if (description == null || !description.containsKey("kind")) {
                        logger.warn("Skipping file {} - missing 'kind'", file.getName());
                        continue;
                    }

                    String kind = (String) description.get("kind");

                    switch (kind) {
                        case "Workflow":
                            WorkflowDescriptor workflowDescriptor = new WorkflowDescriptor();
                            Workflow workflow = workflowDescriptor.fromYamlDescription(content);
                            workflow = workflowRegistry.register(workflow);
                            logger.info("Registered Workflow: {}", workflow.getWorkflowId());
                            break;
                        case "AgentConfig":
                            AgentConfigDescriptor agentDescriptor = new AgentConfigDescriptor();
                            AgentConfig agentConfig = agentDescriptor.fromYamlDescription(content);
                            agentConfigManager.saveConfig(agentConfig);
                            logger.info("Registered AgentConfig: {}", agentConfig.getDomain());
                            break;
                        case "ChatModelConfig":
                            ChatModelConfigDescriptor chatModelDescriptor = new ChatModelConfigDescriptor();
                            ChatModelConfig chatModelConfig = chatModelDescriptor.fromYamlDescription(content);
                            chatModelManager.saveModel(chatModelConfig);
                            logger.info("Registered ChatModelConfig: {}", chatModelConfig.getDomain());
                            break;
                        case "StorageConfig":
                            StorageConfigDescriptor storageDescriptor = new StorageConfigDescriptor();
                            StorageConfig storageConfig = storageDescriptor.fromYamlDescription(content);
                            storageConfigManager.saveConfig(storageConfig);
                            logger.info("Registered StorageConfig: {}", storageConfig.getDomain());
                            break;
                        default:
                            logger.warn("Unknown kind '{}' in file {}", kind, file.getName());
                    }
                } catch (Exception e) {
                    logger.error("Failed to process file {}", file.getName(), e);
                }
            }
        };
    }

    @Bean
    @Order(4)
    public ApplicationRunner orchestrate(Orchestrator service,
                                         WorkflowRegistry workflowRegistry) {
        return args -> {
            List<Workflow> workflows = workflowRegistry.getAll();
            for (Workflow workflow : workflows) {
                logger.info("Orchestrated Workflow: {}", workflow.getWorkflowId());
                service.orchestrate(workflow.getWorkflowId());
            }
        };
    }


}
