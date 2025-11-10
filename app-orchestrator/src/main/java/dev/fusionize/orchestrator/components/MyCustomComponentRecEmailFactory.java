package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "Email Sender Component",
        description = "A component to send email",
        type = MyCustomComponentRecEmail.class,
        compatible = WorkflowNodeType.START
)
public class MyCustomComponentRecEmailFactory implements ComponentRuntimeFactory<MyCustomComponentRecEmail> {
    private final EventPublisher<Event> eventPublisher;
    private final EmailBoxService emailBoxService;

    public MyCustomComponentRecEmailFactory(EventPublisher<Event> eventPublisher,
                                            EmailBoxService emailBoxService) {
        this.eventPublisher = eventPublisher;
        this.emailBoxService = emailBoxService;
    }

    @Override
    public MyCustomComponentRecEmail create() {
        return new MyCustomComponentRecEmail(eventPublisher, emailBoxService);
    }

}
