package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopComponentFactory implements LocalComponentRuntimeFactory<NoopComponent> {

    @Override
    public WorkflowComponent describe() {
        return WorkflowComponent.builder("")
                .withDomain(getName())
                .withName(getName())
                .withDescription("""
**Description**:
The `NoopComponent` (No Operation) does nothing. It immediately succeeds and passes the context through unchanged. It is useful as a placeholder, a bridge, or a starting/ending point in a workflow graph.

**Configuration**:
- *None*

**Context Usage**:
- **Reads/Writes**: Passes the context through without modification.

### Example Config (YAML)
```yaml
# No configuration required
```
""")
                .build();
    }

    @Override
    public String getName() {
        return NoopComponent.NAME;
    }

    @Override
    public NoopComponent create() {
        return new NoopComponent();
    }
}