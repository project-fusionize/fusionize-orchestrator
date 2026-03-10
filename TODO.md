# TODO — Fusionize Orchestrator

> This document serves as the input for creating GitHub Issues.
> Each item includes a title, description, acceptance criteria, and labels.
> Items marked `good first issue` are suitable for new contributors.

---

## Epic 1: Compensation & Error Handling

### 1.1 — Implement compensation node pattern in workflow model

**Labels:** `enhancement`, `core`, `priority: high`

Add support for compensation nodes in the workflow definition. Each workflow node should optionally declare a set of `compensateNodes` — the next nodes to execute when an error occurs during that node's execution.

- Update `WorkflowNode` model to include `compensateNodes` (list of node references)
- Update YAML/JSON workflow descriptor parsing to support `compensate` field
- Update `WorkflowTransformer` to handle compensation edges
- Add unit tests for compensation node parsing

**Module:** `lib-workflow`, `lib-common`

---

### 1.2 — Implement compensation execution in orchestrator

**Labels:** `enhancement`, `core`, `priority: high`

When a component activation or invocation fails, the orchestrator should trigger the compensation path instead of just logging the error.

- In `Orchestrator.java` (lines 80, 106), replace `// todo handle escalation or compensation` with compensation logic
- When a node fails, look up its `compensateNodes` and execute them in order
- Track compensation state in `WorkflowNodeExecution` (e.g., `COMPENSATING`, `COMPENSATED`, `COMPENSATION_FAILED`)
- Persist compensation events in execution logs
- Add integration tests for compensation flow

**Module:** `lib-workflow-orchestration`

---

### 1.3 — Configurable retry with exponential backoff

**Labels:** `enhancement`, `core`, `priority: medium`

Add per-node retry configuration. Before triggering compensation, the orchestrator should retry failed nodes based on configurable policy.

- Add retry config to workflow node model: `maxRetries`, `backoffMs`, `backoffMultiplier`
- Implement retry loop in orchestrator dispatch with exponential backoff
- After max retries exhausted, trigger compensation path
- Log each retry attempt
- Add tests for retry behavior

**Module:** `lib-workflow-orchestration`, `lib-workflow`

---

## Epic 2: Distributed Worker Model

### 2.1 — Define master-worker communication protocol over RabbitMQ

**Labels:** `enhancement`, `architecture`, `priority: high`

Design and implement the event bus protocol so that master (worker0) and remote workers can exchange component activation/invocation messages via RabbitMQ.

- Define message schemas for: `task.dispatch`, `task.result`, `task.error`, `worker.register`, `worker.heartbeat`
- Create exchange/queue topology: `fusionize.tasks.<componentName>`, `fusionize.results`, `fusionize.control`
- Include `executionId`, `stepId`, `correlationId`, `idempotencyKey` in all messages
- Document the protocol in `documents/`

**Module:** `lib-workflow-orchestration`, `lib-workflow-components`

---

### 2.2 — Implement RabbitMQ-based component dispatcher

**Labels:** `enhancement`, `core`, `priority: high`

Extend `OrchestratorComponentDispatcher` to route component invocations through RabbitMQ when the target component is on a remote worker.

- Detect whether a component is local (in-process) or remote (registered by a worker)
- For remote components, publish task message to the component's queue
- Listen on result queue for responses and correlate back to execution
- Fallback: if worker is offline and message expires, trigger compensation

**Module:** `lib-workflow-orchestration`

---

### 2.3 — Worker registration and discovery via RabbitMQ

**Labels:** `enhancement`, `core`, `priority: high`

Enable remote workers (using `worker-spring-boot-starter`) to register their available components with the master on startup.

- Worker publishes `worker.register` message with `workerId`, component list, and capabilities
- Master updates component registry with remote component locations
- Worker sends periodic `worker.heartbeat` messages
- Master marks worker `UNHEALTHY` after missed heartbeats and requeues pending tasks
- Worker deregisters on graceful shutdown

**Module:** `worker-spring-boot-starter`, `lib-workflow-orchestration`

---

### 2.4 — Worker health monitoring and task reassignment

**Labels:** `enhancement`, `priority: medium`

Implement liveness tracking for remote workers.

- Track `lastSeen` timestamp per worker
- Configurable heartbeat interval and miss threshold
- On worker failure: mark unhealthy, reassign or requeue in-flight tasks
- Expose worker health status via REST API (`GET /api/v1.0/workers`)
- Add tests for worker timeout scenarios

**Module:** `lib-workflow-orchestration`

---

## Epic 3: Kubernetes CRD Resource Loading

### 3.1 — Load workflow definitions from Kubernetes CRDs

**Labels:** `enhancement`, `priority: high`, `good first issue`

Enable the master to watch and load workflow YAML definitions from Kubernetes Custom Resources.

- Define CRD schema for `Workflow` resource (kind: `FusionizeWorkflow`)
- Use Kubernetes Java client or fabric8 to watch for CRD events (create/update/delete)
- On CRD event, parse the workflow YAML and register/update in `WorkflowRegistry`
- Support both in-cluster and out-of-cluster kubeconfig modes
- Gracefully degrade when not running in K8s (skip CRD watching)

**Module:** `apps/orchestrator`

---

### 3.2 — Load AgentConfig, ChatModelConfig, StorageConfig from Kubernetes CRDs

**Labels:** `enhancement`, `priority: high`, `good first issue`

Extend CRD loading to cover all resource types currently loaded from `/resources/*.yaml`.

- Define CRD schemas for: `FusionizeAgent`, `FusionizeChatModel`, `FusionizeStorage`
- Watch CRDs and sync to respective config managers (`AgentConfigManager`, `ChatModelManager`, `StorageConfigManager`)
- Handle updates and deletions (sync state)
- Add integration tests with mock K8s API

**Module:** `apps/orchestrator`, `lib-ai`, `lib-storage`

---

## Epic 4: BPMN Process as Master Workflow

### 4.1 — BPMN task-to-sub-workflow linking

**Labels:** `enhancement`, `core`, `priority: high`

Each task in a BPMN process should be able to reference a sub-workflow. The process converter should produce a master workflow that orchestrates sub-workflows.

- Extend BPMN task model to include `workflowRef` (reference to a registered workflow)
- Update `lib-process` converters to emit a master workflow node that triggers sub-workflow execution
- Sub-workflow execution should be tracked as a child of the parent execution
- Support passing context between parent and sub-workflow

**Module:** `lib-process`, `lib-workflow-orchestration`

---

### 4.2 — Sub-workflow execution lifecycle management

**Labels:** `enhancement`, `core`, `priority: medium`

The orchestrator must manage sub-workflow lifecycle: start, monitor, collect results, and handle failures.

- Parent workflow node waits until sub-workflow completes
- Sub-workflow results merge back into parent context
- Sub-workflow failure triggers parent node compensation
- Support parallel sub-workflows (via BPMN parallel gateway)
- Add tests for nested workflow execution

**Module:** `lib-workflow-orchestration`

---

## Epic 5: Blueprint Versioning & Lifecycle

### 5.1 — Workflow versioning model

**Labels:** `enhancement`, `priority: medium`, `good first issue`

Each workflow change should create a new immutable version.

- Add `version` field to workflow model (auto-increment or hash-based)
- Store all versions in MongoDB; never overwrite
- New executions bind to the currently `active` version
- Running executions remain pinned to their version
- API: `GET /api/v1.0/workflow/{id}/versions` — list all versions

**Module:** `lib-workflow`, `apps/orchestrator`

---

### 5.2 — Workflow activation and dry-run endpoints

**Labels:** `enhancement`, `priority: medium`, `good first issue`

Add API endpoints for lifecycle management.

- `PATCH /api/v1.0/workflow/{id}/activate?version=N` — set active version for new executions
- `POST /api/v1.0/workflow/{id}/dryrun` — validate a workflow definition without executing
- Emit `blueprintUpdate` event when active version changes
- Add controller tests

**Module:** `apps/orchestrator`

---

## Epic 6: Human Task Support (Headless)

### 6.1 — Human task node type and execution pause/resume

**Labels:** `enhancement`, `core`, `priority: medium`

Implement headless human task support. The orchestrator pauses execution and exposes pending human tasks via API.

- Define `HUMAN` task node type in workflow model (actor = `HUMAN`)
- When orchestrator reaches a human task node, set execution state to `PAUSED` / `AWAITING_HUMAN`
- Persist human task item with: `assignee`, `role`, `description`, `deadline`
- API: `GET /api/v1.0/tasks/human` — list pending human tasks
- API: `POST /api/v1.0/tasks/human/{taskId}/complete` — submit decision and resume execution
- Emit events on RabbitMQ for external consumers (e.g., fusionize-hub)

**Module:** `lib-workflow-orchestration`, `apps/orchestrator`

---

### 6.2 — Human task escalation on timeout

**Labels:** `enhancement`, `priority: low`

If a human task exceeds its deadline, the orchestrator should escalate.

- Scheduled job checks for overdue human tasks
- Apply escalation rules from workflow definition (e.g., reassign to different role)
- If no escalation rule, trigger compensation path
- Log escalation events

**Module:** `lib-workflow-orchestration`

---

## Epic 7: AI Task Confidence Routing

### 7.1 — AI task confidence threshold and fallback routing

**Labels:** `enhancement`, `priority: medium`

AI components should return a `confidence` score. Below a configurable threshold, the orchestrator routes to a fallback path (e.g., human review).

- Extend AI component response model to include `confidence` and `explanation`
- Add `confidenceThreshold` config to AI task nodes in workflow definition
- Orchestrator checks confidence after AI invocation; if below threshold, route to fallback node
- Store prompt, model id, response, and confidence in audit log
- Add tests for confidence routing

**Module:** `lib-workflow-orchestration`, `lib-ai`

---

## Epic 8: Dynamic Workflow Modification

### 8.1 — Per-execution workflow patch API

**Labels:** `enhancement`, `priority: medium`

Allow modifying a running workflow execution's remaining steps.

- `PATCH /api/v1.0/workflow/{workflowId}/executions/{executionId}` with operations: `addStep`, `removeStep`, `updateStep`, `setNextStep`, `patchContext`
- Validate patches (no orphaned steps, valid node references)
- Store reverse patch for rollback
- Log all patches with actor, reason, timestamp, diff
- Add controller and integration tests

**Module:** `apps/orchestrator`, `lib-workflow-orchestration`

---

## Epic 9: Observability & Metrics

### 9.1 — Prometheus metrics endpoint

**Labels:** `enhancement`, `priority: medium`, `good first issue`

Expose key orchestrator metrics via Micrometer/Prometheus.

- Add `micrometer-registry-prometheus` dependency
- Expose metrics: `fusionize_executions_active`, `fusionize_executions_total`, `fusionize_nodes_completed`, `fusionize_nodes_failed`, `fusionize_workers_healthy`, `fusionize_workers_total`, `fusionize_component_invocation_duration`
- Expose `/actuator/prometheus` endpoint
- Document available metrics

**Module:** `apps/orchestrator`

---

### 9.2 — OpenTelemetry tracing integration

**Labels:** `enhancement`, `priority: low`, `good first issue`

Add distributed tracing across orchestrator and components.

- Add OpenTelemetry Spring Boot starter dependency
- Instrument orchestrator dispatch, component invocation, and workflow navigation with spans
- Propagate trace context through RabbitMQ message headers
- Document how to connect to Jaeger/Zipkin

**Module:** `apps/orchestrator`, `lib-workflow-orchestration`

---

## Epic 10: Developer Experience

### 10.1 — Docker image build for orchestrator

**Labels:** `enhancement`, `devex`, `priority: medium`, `good first issue`

Create a Dockerfile and integrate into CI.

- Add multi-stage Dockerfile (build with Gradle, run with JRE 21)
- Add image build step to GitHub Actions workflow
- Publish to GitHub Container Registry (ghcr.io)
- Update docker-compose to optionally use the built image

**Module:** root, `.github/workflows`

---

### 10.2 — Improve CI pipeline

**Labels:** `enhancement`, `devex`, `priority: medium`, `good first issue`

Extend GitHub Actions to include test reporting and quality checks.

- Add test result reporting (JUnit XML → GitHub summary)
- Add code coverage reporting (JaCoCo)
- Add linting / checkstyle step
- Run on PRs with status checks

**Module:** `.github/workflows`

---

### 10.3 — Contributing guide and local dev setup docs

**Labels:** `documentation`, `devex`, `priority: low`, `good first issue`

Create contributor-friendly documentation.

- `CONTRIBUTING.md` — how to build, test, run locally
- Document docker-compose setup and required environment variables
- Document module structure and where to add new components
- Add architecture diagram (Mermaid or image)

**Module:** root

---

### 10.4 — Remove hardcoded secrets and improve config defaults

**Labels:** `bug`, `devex`, `priority: medium`, `good first issue`

Clean up configuration for open-source readiness.

- Remove `api-key: "fake"` from `application.yml`; require env var `OPENAI_API_KEY`
- Add `.env.example` with all required environment variables documented
- Ensure app starts without AI features if no API key is set (graceful degradation)
- Verify no credentials are committed in resource files

**Module:** `apps/orchestrator`

---

## Epic 11: Testing

### 11.1 — End-to-end workflow execution test

**Labels:** `testing`, `priority: high`

Create integration tests that execute a complete workflow from start to end.

- Test a simple linear workflow (start → task → end)
- Test a decision workflow (start → decision → branch A / branch B → end)
- Test a parallel workflow (start → fork → [tasks] → join → end)
- Use embedded MongoDB and test containers
- Verify execution state, context propagation, and logs

**Module:** `lib-workflow-orchestration`, `lib-common-test`

---

### 11.2 — Compensation flow integration tests

**Labels:** `testing`, `priority: high`

Test the compensation pattern end-to-end.

- Simulate component failure → verify compensation nodes execute
- Test retry exhaustion → compensation trigger
- Test nested compensation (sub-workflow failure)

**Module:** `lib-workflow-orchestration`

---

### 11.3 — RabbitMQ integration tests

**Labels:** `testing`, `priority: medium`

Test message broker communication using Testcontainers.

- Test task dispatch and result correlation
- Test worker registration and heartbeat
- Test message persistence when worker is offline

**Module:** `lib-workflow-orchestration`

---

## Summary — Suggested Priority Order

| Priority | Items |
|----------|-------|
| **P0 — Foundation** | 1.1, 1.2, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 11.1 |
| **P1 — Core Features** | 1.3, 2.4, 4.2, 5.1, 5.2, 6.1, 7.1, 10.1, 10.4, 11.2 |
| **P2 — Polish** | 8.1, 9.1, 9.2, 10.2, 10.3, 11.3 |
| **P3 — Future** | 6.2 |

### Good First Issues

- 3.1 — Load workflow definitions from Kubernetes CRDs
- 3.2 — Load AgentConfig, ChatModelConfig, StorageConfig from Kubernetes CRDs
- 5.1 — Workflow versioning model
- 5.2 — Workflow activation and dry-run endpoints
- 9.1 — Prometheus metrics endpoint
- 9.2 — OpenTelemetry tracing integration
- 10.1 — Docker image build for orchestrator
- 10.2 — Improve CI pipeline
- 10.3 — Contributing guide and local dev setup docs
- 10.4 — Remove hardcoded secrets and improve config defaults
