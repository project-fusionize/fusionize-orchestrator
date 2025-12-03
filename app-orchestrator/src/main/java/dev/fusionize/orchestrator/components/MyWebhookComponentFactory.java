package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.WebhookService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(name = "Webhook Component", description = "A component that waits for a webhook callback", type = MyWebhookComponent.class, compatible = WorkflowNodeType.START)
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
