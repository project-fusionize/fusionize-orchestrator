package dev.fusionize.web;


import dev.fusionize.web.services.WebhookService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(type = Webhook.class, name = "webhook", description = "Webhook component", actors = {
        Actor.SYSTEM })
public class WebhookFactory implements ComponentRuntimeFactory<Webhook> {
    private final WebhookService webhookService;

    public WebhookFactory(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public Webhook create() {
        return new Webhook(webhookService);
    }
}
