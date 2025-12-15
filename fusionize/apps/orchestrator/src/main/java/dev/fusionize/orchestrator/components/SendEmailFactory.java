package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(type = SendEmail.class,
        domain = "test.sendEmail",
        name = "test send email",
        description = "Send email component",
        actors = { Actor.SYSTEM })
public class SendEmailFactory implements ComponentRuntimeFactory<SendEmail> {

    @Override
    public SendEmail create() {
        return new SendEmail();
    }

}
