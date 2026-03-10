package dev.fusionize.web;

import dev.fusionize.web.services.HttpOutboundConnectorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        type = HttpOutboundConnector.class,
        domain = "fuz.connector.HttpOutbound",
        name = "Http Outbound Connector",
        description = "Makes HTTP requests to external APIs and services",
        actors = {Actor.SYSTEM})
public class HttpOutboundConnectorFactory implements ComponentRuntimeFactory<HttpOutboundConnector> {
    private final HttpOutboundConnectorService httpOutboundConnectorService;

    public HttpOutboundConnectorFactory(HttpOutboundConnectorService httpOutboundConnectorService) {
        this.httpOutboundConnectorService = httpOutboundConnectorService;
    }

    @Override
    public HttpOutboundConnector create() {
        return new HttpOutboundConnector(httpOutboundConnectorService);
    }
}
