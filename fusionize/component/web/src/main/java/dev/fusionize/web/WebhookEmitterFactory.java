package dev.fusionize.web;

import dev.fusionize.web.services.WebhookEmitterService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        type = WebhookEmitter.class,
        domain = "fuz.connector.WebhookEmitter",
        name = "Webhook Emitter",
        description = "Sends webhook notifications to external URLs with optional HMAC signing",
        actors = {Actor.SYSTEM})
public class WebhookEmitterFactory implements ComponentRuntimeFactory<WebhookEmitter> {
    private final WebhookEmitterService webhookEmitterService;

    public WebhookEmitterFactory(WebhookEmitterService webhookEmitterService) {
        this.webhookEmitterService = webhookEmitterService;
    }

    @Override
    public WebhookEmitter create() {
        return new WebhookEmitter(webhookEmitterService);
    }
}
