package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class ForkComponentFactory implements LocalComponentRuntimeFactory<ForkComponent> {
    @Override
    public WorkflowComponent describe() {
        return WorkflowComponent.builder("")
                .withDomain(getName())
                .withName(getName())
                .withDescription("""
**Description**:
The `ForkComponent` evaluates a set of conditions to determine which outgoing path(s) the workflow should follow. It supports different evaluation engines (SpEL, JavaScript, Kotlin, Groovy) and fork modes.

**Configuration**:
- `conditions` (Map<String, String>): A map where keys are target node names and values are boolean expressions.
- `forkMode` (String):
  - `INCLUSIVE` (Default): All matching paths are executed.
  - `EXCLUSIVE`: Only the first matching path is executed.
- `default` (String): The node name to follow if no conditions are met.
- `parser` (String): The expression engine to use. Options: `spel` (Default), `js`, `kotlin`, `groovy`, `kts`.

**Context Usage**:
- **Reads**: Uses the entire workflow context variables map to evaluate the expressions.
- **Writes**: updates `Decision` in context options.

### Example Config (YAML)
```yaml
parser: "spel"
forkMode: "EXCLUSIVE"
default: "fallbackNode"
conditions:
  pathA: "context['retryCount'] > 3"
  pathB: "context['status'] == 'active'"
```
""")
                .build();
    }

    @Override
    public String getName() {
        return ForkComponent.NAME;
    }

    @Override
    public ForkComponent create() {
        return new ForkComponent();
    }
}
