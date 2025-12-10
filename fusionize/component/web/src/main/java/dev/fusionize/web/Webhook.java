package dev.fusionize.web;

import dev.fusionize.web.services.WebhookService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Webhook implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(Webhook.class);
    private final WebhookService webhookService;

    public Webhook(WebhookService webhookService) {
        this.webhookService = webhookService;
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
        String workflowKey = context.getRuntimeData().getWorkflowId();
        String workflowNodeKey = context.getRuntimeData().getWorkflowNodeKey();

        if (workflowKey == null || workflowNodeKey == null) {
            emitter.failure(new IllegalStateException("WorkflowKey and WorkflowNodeKey are required"));
            return;
        }

        WebhookService.WebhookKey key = new WebhookService.WebhookKey(workflowKey, workflowNodeKey);

        logger.info("Registering webhook listener for key: {}", key);

        webhookService.addListener(key, body -> {
            logger.info("Webhook triggered for key: {}, body: {}", key, body);
            context.getData().putAll(body);
            webhookService.removeListener(key); // Cleanup
            emitter.success(context);
        });
    }
}
