package dev.fusionize.orchestrator.config;

import dev.fusionize.orchestrator.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String URL_SOCKET_BASE = "/socket/"+ Application.VERSION;
    public static final String URL_NODE_TOPIC_BASE = "/topic/"+ Application.VERSION + ".node";
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${spring.rabbitmq.host:#{null}}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.virtual-host:#{null}}")
    private String rabbitmqVirtualHost;

    @Value("${spring.rabbitmq.stomp.port:0}")
    private int stompPort;

    @Value("${spring.rabbitmq.username:#{null}}")
    private String username;

    @Value("${spring.rabbitmq.password:#{null}}")
    private String password;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if(stompPort==0) return;
        try {
            StompBrokerRelayRegistration brokerRelayRegistration =
                    registry.enableStompBrokerRelay("/topic/")
                    .setRelayHost(rabbitmqHost)
                    .setVirtualHost(rabbitmqVirtualHost)
                    .setClientLogin(username)
                    .setClientPasscode(password)
                    .setSystemLogin(username)
                    .setSystemPasscode(password);
            if(rabbitmqVirtualHost!=null && !rabbitmqVirtualHost.isEmpty()) {
                brokerRelayRegistration.setVirtualHost(rabbitmqVirtualHost);
            }
            registry.setApplicationDestinationPrefixes("/app");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(URL_SOCKET_BASE).setAllowedOrigins("*");
    }
}
