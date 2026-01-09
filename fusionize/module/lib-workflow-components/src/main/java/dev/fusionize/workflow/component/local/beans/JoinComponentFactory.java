package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;
import org.springframework.stereotype.Component;

@Component
public class JoinComponentFactory implements LocalComponentRuntimeFactory<JoinComponent> {
    private final WorkflowExecutionRegistry workflowExecutionRegistry;

    public JoinComponentFactory(WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }


    @Override
    public WorkflowComponent describe() {
        return WorkflowComponent.builder("")
                .withDomain(getName())
                .withName(getName())
                .withDescription("""
**Description**:
The `JoinComponent` synchronizes multiple parallel execution paths. It waits for a specific set of parent nodes (`await` list) to complete before proceeding. It merges the data from the contexts of the incoming branches based on a configured strategy.

**Configuration**:
- `await` (List<String>): A list of node IDs (names) that this component must wait for.
- `mergeStrategy` (String):
  - `PICK_LAST` (Default): If keys collide, the value from the last processed context overwrites previous ones.
  - `PICK_FIRST`: The value from the first processed context is retained.
- `waitMode` (String):
  - `ALL` (Default): Waits for all nodes in the `await` list.
  - `ANY`: Proceeds if any one of the awaited nodes completes.
  - `THRESHOLD`: Proceeds if a specific number of unique awaited nodes complete (requires `thresholdCount`).
- `thresholdCount` (Integer): The number of nodes to wait for when `waitMode` is `THRESHOLD`.

**Context Usage**:
- **Reads**: Inspects execution history of incoming contexts to determine if awaited nodes have completed.
- **Writes**: Merges data, decisions, and graph execution history from all incoming branches into a single new context.

### Example Config (YAML)
```yaml
await:
  - "branchA"
  - "branchB"
waitMode: "ALL"
mergeStrategy: "PICK_LAST"
```
""")
                .build();
    }

    @Override
    public String getName() {
        return JoinComponent.NAME;
    }

    @Override
    public JoinComponent create() {
        return new JoinComponent(this.workflowExecutionRegistry);
    }
}
