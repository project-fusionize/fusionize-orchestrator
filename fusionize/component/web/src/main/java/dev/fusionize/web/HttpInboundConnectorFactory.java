package dev.fusionize.web;


import dev.fusionize.web.services.HttpInboundConnectorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        type = HttpInboundConnector.class,
        name = "Http Inbound Connector",
        description = "Http Inbound Connector component",
        actors = { Actor.SYSTEM })
public class HttpInboundConnectorFactory implements ComponentRuntimeFactory<HttpInboundConnector> {
    private final HttpInboundConnectorService httpInboundConnectorService;

    public HttpInboundConnectorFactory(HttpInboundConnectorService httpInboundConnectorService) {
        this.httpInboundConnectorService = httpInboundConnectorService;
    }

    @Override
    public HttpInboundConnector create() {
        return new HttpInboundConnector(httpInboundConnectorService);
    }
}
