package dev.fusionize.web;

import dev.fusionize.web.services.HttpInboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpInboundConnector implements ComponentRuntime {
    private final HttpInboundConnectorService httpInboundConnectorService;

    public HttpInboundConnector(HttpInboundConnectorService httpInboundConnectorService) {
        this.httpInboundConnectorService = httpInboundConnectorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        String workflowKey = context.getRuntimeData().getWorkflowDomain();
        workflowKey = workflowKey == null ? context.getRuntimeData().getWorkflowId() : workflowKey;
        String workflowNodeKey = context.getRuntimeData().getWorkflowNodeKey();

        if (workflowKey == null || workflowNodeKey == null) {
            emitter.failure(new IllegalStateException("WorkflowKey and WorkflowNodeKey are required"));
            return;
        }

        HttpInboundConnectorService.HttpConnectorKey key = new HttpInboundConnectorService.HttpConnectorKey(workflowKey, workflowNodeKey);

        emitter.logger().info("Registering http listener for key: {}", key);

        httpInboundConnectorService.addListener(key, body -> {
            emitter.logger().info("Http listener triggered for key: {}, body: {}", key, body);
            context.getData().putAll(body);
//            httpInboundConnectorService.removeListener(key); // Cleanup
            emitter.success(context);
        });
    }
}
