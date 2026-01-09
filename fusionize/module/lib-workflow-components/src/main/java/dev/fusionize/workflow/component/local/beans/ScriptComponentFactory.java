package dev.fusionize.workflow.component.local.beans;


import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
public class ScriptComponentFactory implements LocalComponentRuntimeFactory<ScriptComponent> {
    @Override
    public WorkflowComponent describe() {
        return WorkflowComponent.builder("")
                .withDomain(getName())
                .withName(getName())
                .withDescription("""
**Description**:
The `ScriptComponent` executes a script using a specified language engine (JavaScript, Kotlin, or Groovy). The script has full access to the workflow context and a logger. It can modify the context variables.

**Configuration**:
- `script` (String): The source code of the script to execute.
- `parser` (String): The script engine to use. Options: `js` (Default), `kotlin`, `groovy`, `kts`.

**Context Usage**:
- **Reads/Writes**:
  - The script has access to a `context` object (which is a proxy to the workflow context data).
  - The script can read variables from `context` and write to it.
  - If the script returns a `Map`, those values are merged back into the workflow context.
- **Resources**:
  - `logger`: A `LoggerWrapper` is provided to the script (methods: `info`, `warn`, `error`, `debug`, `log`).

### Example Config (YAML)
```yaml
parser: "js"
script: |
  logger.info("Running script...");
  var count = context.count || 0;
  context.count = count + 1;
  // Return value is optional; if it's a map/object, it merges with context
  ({ "updatedBy": "script-node" })
```
""")
                .build();
    }

    @Override
    public String getName() {
        return ScriptComponent.NAME;
    }

    @Override
    public ScriptComponent create() {
        return new ScriptComponent();
    }
}

