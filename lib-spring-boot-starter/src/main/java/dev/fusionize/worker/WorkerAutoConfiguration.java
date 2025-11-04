package dev.fusionize.worker;

import dev.fusionize.worker.oidc.OidcTokenClient;
import dev.fusionize.worker.stomp.WorkerStompSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Configuration
@ConditionalOnClass(Worker.class)
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(WorkerAutoConfiguration.class);

    private final WorkerProperties workerProperties;

    public WorkerAutoConfiguration(WorkerProperties workerProperties) {
        this.workerProperties = workerProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public Worker greeter() {
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
    public ApplicationRunner connectStompClient(OidcTokenClient oidcTokenClient,
                                                WebSocketStompClient stompClient,
                                                StompSessionHandler sessionHandler) {
        return args -> {
            String url = WorkerProperties.toWebSocketUrl(workerProperties.getOrchestratorUrl()) + "/ws/1.0";
            logger.info("Connecting to WebSocket STOMP server at {}", url);
            try {
                String clientToken = oidcTokenClient.getClientCredentialsToken(
                        workerProperties.getWorkerOidcClientId(), workerProperties.getWorkerOidcClientSecret(), null, null
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

}
