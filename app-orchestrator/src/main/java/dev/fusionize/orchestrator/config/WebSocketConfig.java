package dev.fusionize.orchestrator.config;

import dev.fusionize.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.net.URI;
import java.net.URL;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String URL_SOCKET_BASE = "/ws/"+ Application.VERSION;
    public static final String URL_NODE_TOPIC_BASE = "/topic/"+ Application.VERSION + ".node";
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${fusionize.worker.orchestrator-amqp:#{null}}")
    private String amqConnectionString;

    private static class BrokerRelayConnectionInfo {
        final String host;
        final String vhost;
        final String user;
        final String password;

        BrokerRelayConnectionInfo(String connectionString) throws Exception {
            connectionString = connectionString.replaceFirst("[a-z]+://", "https://");
            URL url = URI.create(connectionString).toURL();
            this.host = url.getHost();
            this.vhost = url.getFile().replaceFirst("/","");
            String userInfo = url.getUserInfo();
            if(userInfo==null){
                this.user = "";
                this.password = "";
            }else{
                this.user = userInfo.split(":")[0];
                this.password = userInfo.split(":")[1];
            }
        }

        @Override
        public String toString() {
            return "BrokerRelayConnectionInfo{" +
                    "host='" + host + '\'' +
                    ", vhost='" + vhost + '\'' +
                    '}';
        }
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if(amqConnectionString==null) return;
        try {
            BrokerRelayConnectionInfo connectionInfo = new BrokerRelayConnectionInfo(amqConnectionString);
            logger.info(connectionInfo.toString());
            StompBrokerRelayRegistration brokerRelayRegistration =
                    registry.enableStompBrokerRelay("/topic/")
                            .setRelayHost(connectionInfo.host)
                            .setClientLogin(connectionInfo.user)
                            .setClientPasscode(connectionInfo.password)
                            .setSystemLogin(connectionInfo.user)
                            .setSystemPasscode(connectionInfo.password);
            if(!connectionInfo.vhost.isEmpty()) {
                brokerRelayRegistration.setVirtualHost(connectionInfo.vhost);
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
