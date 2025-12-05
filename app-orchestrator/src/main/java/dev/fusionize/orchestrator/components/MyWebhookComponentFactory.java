package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.WebhookService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(type = MyWebhookComponentFactory.class, name = "webhook", description = "Webhook component", actors = {
        Actor.SYSTEM })
public class MyWebhookComponentFactory implements ComponentRuntimeFactory<MyWebhookComponent> {
    private final WebhookService webhookService;

    public MyWebhookComponentFactory(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public MyWebhookComponent create() {
        return new MyWebhookComponent(webhookService);
    }
}
