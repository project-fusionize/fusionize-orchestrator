package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "Email Sender Component",
        description = "A component to send email",
        type = MyCustomComponentRecEmail.class,
        compatible = WorkflowNodeType.START
)
public class MyCustomComponentRecEmailFactory implements ComponentRuntimeFactory<MyCustomComponentRecEmail> {
    private final EmailBoxService emailBoxService;

    public MyCustomComponentRecEmailFactory(EmailBoxService emailBoxService) {
        this.emailBoxService = emailBoxService;
    }

    @Override
    public MyCustomComponentRecEmail create() {
        return new MyCustomComponentRecEmail(emailBoxService);
    }

}
