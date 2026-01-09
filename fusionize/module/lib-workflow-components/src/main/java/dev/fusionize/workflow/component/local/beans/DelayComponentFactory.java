package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class DelayComponentFactory implements LocalComponentRuntimeFactory<DelayComponent> {

    @Override
    public WorkflowComponent describe() {
        return WorkflowComponent.builder("")
                .withDomain(getName())
                .withName(getName())
                .withDescription("""
**Description**:
The `DelayComponent` pauses the workflow execution for a specified duration. It uses a shared `ScheduledExecutorService` to handle the wait time without blocking a thread. Once the delay executes, it updates the context and proceeds.

**Configuration**:
- `delay` (Integer): The duration to wait in milliseconds. Defaults to 5000 (5 seconds) if not specified.

**Context Usage**:
- **Writes**: Sets a variable named `delayed` in the workflow context containing the integer value of the delay used.

### Example Config (YAML)
```yaml
delay: 10000 # Wait for 10 seconds
```
""")
                .build();
    }

    @Override
    public String getName() {
        return DelayComponent.NAME;
    }

    @Override
    public DelayComponent create() {
        return new DelayComponent();
    }
}
