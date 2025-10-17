# SPEC.MD — Fusionize Orchestrator: Technical Specification

> Detailed technical specification for the Fusionize control plane (fusionize-orchestrator), node registration and authentication, runtime communications (WebSocket + MQ), health & lifecycle, blueprint deployment & versioning, task types (AI vs Human), dynamic workflow edits, observability, and operational concerns.

---

## Table of contents

1. Goals & scope
2. High-level runtime model
3. Component registration & trust bootstrap (RSA + tokens)
4. Persistent connection: WebSocket (control) layered with MQ (work)
5. Health checks, heartbeats, and liveness model
6. Registry & metadata model (MongoDB)
7. Blueprint lifecycle, storage, and deployment model
8. Execution model: tasks, steps, context propagation
9. Human task vs AI task (semantics & flow)
10. Dynamic workflow modification (per-execution & global)
11. Message formats & channels (JSON schemas)
12. Error handling, retries, compensation patterns
13. Observability, audit, and security considerations
14. Scaling, HA, and deployment notes
15. Backwards compatibility & versioning strategy
16. Future extensions & optional integrations

---

## 1. Goals & scope

* Provide an explicit, actionable specification for building the master (orchestrator) and node interactions.
* Ensure secure, reliable registration & authentication for components/blueprints.
* Support real-time control (WebSocket) and durable task delivery (message broker) as pluggable options.
* Enable dynamic, AI-assisted edits to running workflows and blueprint definitions.
* Provide clear semantics for Human tasks vs AI tasks and how they are executed, tracked, and audited.

---

## 2. High-level runtime model

* **Orchestrator (master)**: single (or clustered) control plane. Responsibilities: registry, auth, WS endpoint, MQ bridge, blueprint loader, execution coordinator, audit log, UI/API.
* **Components**: independent MCP-like microservices that implement `/metadata`, `/invoke`, `/ping` (conceptually). They register with orchestrator and can be invoked by workflows.
* **Blueprint Runtime**: a shared execution service (process runner) that loads/executes blueprint definitions and interacts with components via orchestrator.
* **Communication**:

    * **Control channel**: WebSocket (STOMP or lightweight JSON frames) between orchestrator ↔ node for commands, updates, logs, and low-latency notifications.
    * **Work channel**: Message broker (RabbitMQ optional by deployment) used for durable task queues, replay, and scaling. Orchestrator routes tasks either directly over WS or onto MQ.
* **Storage**: MongoDB for registry, blueprint definitions, execution state, events. Optional Postgres for structured audit/tabled reports.

---

## 3. Component registration & trust bootstrap (RSA + tokens)

### Goals

* Ensure that only legitimate components/blueprints register.
* Prevent MITM and replay attacks.
* Keep node lifecycle manageable (revocation, rotation, short-lived tokens).

### Entities

* `MASTER` (control plane): holds RSA keypair (`master_private`, `master_public`).
* `NODE` (component or blueprint runtime): generates its own keypair (`node_private`, `node_public`) or may be pre-provisioned.

### Bootstrap flow (sequence)

1. **Master public key exposure**: `GET /api/v1/public-key` — returns `master_public`. Public endpoint; no auth.
2. **Node key generation**: on first startup, node generates `node_keypair` locally.
3. **Registration payload**: node composes `registration` JSON including `nodeId`, `nodeType`, `capabilities`, `metadata`, `node_public`, `timestamp`.
4. **Signature & encryption**:

    * Node signs the payload with its `node_private` (signature ensures authenticity of node).
    * Optionally node encrypts payload using `master_public` (ensures confidentiality).
5. **POST /api/v1/register** with signed (and optionally encrypted) payload.
6. **Master validation**:

    * Master decrypts (if necessary), verifies signature using provided `node_public`.
    * If valid and policy checks pass, master registers node in registry (status = `REGISTERED`) and issues:

        * Short-lived **auth token** (JWT signed by master_private) with claims `{ nodeId, exp, capabilities, nonce }`.
        * Optionally issue TLS certificate or mTLS artifact for long-term secure comms.
7. **Response**: master returns `{ token, nodeId, config }`.

### Token lifecycle & rotation

* Tokens are **short-lived** (e.g., 5–30 minutes) and used to authenticate WebSocket upgrade and MQ client auth.
* Node uses token to open WebSocket: `ws://master/ws?token=<JWT>` or include in STOMP CONNECT headers.
* Master exposes `POST /api/v1/refresh-token` for node to exchange a valid token + signed nonce for a new token (requires node signature).
* Master supports **revocation**: blacklist of `nodeId` or token `jti`. Revocation used on compromise.

### Security notes

* Use RSA2048+ or recommended modern curve keys for production (ECDSA/P-256 or ED25519 may be preferred).
* Store `master_private` in secure vault; provide rotation and automatic invalidation.
* Log registration attempts with sufficient metadata for auditing.

---

## 4. Persistent connection: WebSocket (control) layered with MQ (work)

### Control channel (WebSocket)

* Purpose: low-latency bi-directional control messages:

    * Node heartbeats, health updates, logs (streaming).
    * Master-initiated control commands: config update, immediate triggers, cancellations, administrative messages.
    * Live subscription topics for blueprint events and admin overrides.
* Protocol choices:

    * Plain WebSocket + JSON frames (simple).
    * STOMP over WebSocket (if using topic/queue semantics built into STOMP).
    * gRPC-Web / HTTP2 streams (alternative).
* Authentication: WebSocket handshake must include JWT token (query param or header). Master validates JWT and node identity prior to accepting messages.

### Work channel (Message Broker)

* Purpose: durable, scalable distribution of tasks and events.
* Broker choices: RabbitMQ (AMQP), NATS JetStream, or Kafka.
* Integration patterns:

    * **Direct dispatch**: master publishes task to `task_exchange` or `tasks.<componentName>` with routing key. Component consumes from queue.
    * **Fallback & replay**: if node is offline, message persists; when node reconnects, it consumes pending tasks.
    * **Hybrid mode**: master pushes immediate tasks over WS for active nodes; for nodes offline or for durable processing, master publishes onto MQ.
* Broker authentication: nodes connect with credentials issued by master (or secrets). For RabbitMQ, master can provision user/virtual-host or use shared credentials plus queue-per-node.

### Routing strategy

* Components registered with multiple instances get per-instance queues or a shared `tasks.<component>` queue with competing consumers.
* Blueprints that require exclusive runner use `executions.<executionId>` queues for directed messages.
* Use a **correlation id** (executionId) in message headers to link all messages for an execution.

---

## 5. Health checks, heartbeats, and liveness model

### Health heartbeat (WS)

* Node sends heartbeat every `T` seconds (e.g., 10–30s): `{ type: "heartbeat", nodeId, status: "healthy", timestamp, metrics }`.
* Master updates `lastSeen` and TTL for node. If no heartbeat after `N` intervals (e.g., 3 misses), mark node `UNHEALTHY` and optionally requeue or reassign tasks.

### Active health probe

* Master periodically calls `/mcp/ping` on component endpoints (HTTP) for additional checks (latency, dependency health).
* For MQ-backed tasks, master monitors queue message age and consumer lag for liveness.

### Health UI & alerts

* Orchestrator exposes metrics:

    * `nodes.total`, `nodes.healthy`, `nodes.unhealthy`
    * per-component `queue_depth`, `avg_processing_time`, `error_rate`
* Integrate Prometheus + Grafana. Trigger alerts for `queue_depth > threshold` or stale heartbeats.

---

## 6. Registry & metadata model (MongoDB)

### Collections

* `components`:

    * `_id` (componentId), `name`, `type`, `capabilities`, `endpoint`, `publicKey`, `status`, `lastSeen`, `versions`, `metadata`
* `blueprints`:

    * `_id` (blueprintId), `version`, `definition` (JSON/YAML), `author`, `createdAt`, `status` (draft/active/deprecated)
* `executions`:

    * `_id` (execId), `blueprintId`, `blueprintVersion`, `state`, `context` (JSON), `startedAt`, `updatedAt`, `logs`
* `events` (audit/log store): time-series of events for traceability
* `tokens` (optional): active tokens and revocation list

### Key queries

* Find component by capability: `components.find({ capabilities: { $in: ["ai:classify"] }})`
* Active executions by blueprint: `executions.find({ blueprintId, state: { $in: ["running","waiting"] } })`

---

## 7. Blueprint lifecycle, storage, and deployment model

### Blueprint model

* Versioned: each blueprint change creates a new version. Immutable versions enable reproducible execution and audit.
* Definition types supported:

    * **Declarative DSL (JSON/YAML)** — recommended (serverless-workflow-inspired).
    * **Workflow-as-Code** — JS/TS or JVM-based SDK compile to the declarative format (SDKs produce canonical JSON).
* Metadata includes `editable` flag, `permissions`, `autoDeploy` policy.

### Deployment & GitOps

* Blueprints are stored in registry (Mongo) and can be imported from Git repositories.
* CI/CD: Git pushes to a `blueprint` repo trigger validation, unit tests, and optional auto-deploy to orchestrator.
* APIs:

    * `POST /api/v1/blueprints` — upload new blueprint version (validate and store).
    * `PATCH /api/v1/blueprints/{id}/activate` — set default version for new executions.
    * `POST /api/v1/blueprints/{id}/dryrun` — validate without executing.

### Hot-reload & per-execution binding

* New executions use the currently `active` blueprint version. Running executions stay linked to their version.
* Orchestrator keeps blueprint versions cached; changes to `active` version publish a `blueprintUpdate` event to subscribers.
* Hot patching: admin can patch an **individual execution** by applying a runtime override (see Dynamic Workflow Modification).

---

## 8. Execution model: tasks, steps, context propagation

### Execution unit

* **Execution** (process instance) is the root object. It holds `context` (mutable JSON object) which flows through steps.

### Step types

* **Task**: invokes a component (AI or non-AI), waits for response, updates context.
* **Decision**: evaluates rules or calls an AI decision component to route next step.
* **Parallel**: runs forked substeps concurrently.
* **Timer**: wait/sleep until certain time or timeout.
* **HumanTask**: pause execution until human completes decision via UI.

### Context

* Each step can read/transform `context`. Context is persisted after each step to `executions` collection.
* Sensitive fields should be marked and optionally encrypted at rest.

### Invocation semantics

* Synchronous over WS: orchestrator sends `invoke` control message; node executes and responds.
* Asynchronous over MQ: orchestrator publishes task message to MQ; node consumes and replies on a `reply-to` or `executions.{execId}` queue.
* All messages include correlation id = `executionId`, `stepId`, and a `requestId`.

---

## 9. Human task vs AI task (semantics & flow)

### AI Task (automated)

* `taskType: ai`
* Execution flow:

    1. Orchestrator identifies AI component (local LLM adapter or external provider).
    2. Orchestrator composes prompt/inputs and invokes AI component via `invoke`.
    3. AI component returns result + `confidence` and optional `explanation`.
    4. Orchestrator uses `confidence` threshold:

        * If `>= threshold` → continue.
        * If `< threshold` → route to HumanTask or fallback path.
* Auditing: store prompt, model id, response, and confidence for provenance.

### Human Task (human-in-loop)

* `taskType: human`
* Execution flow:

    1. Orchestrator creates a human task item and persists it.
    2. Notification: publish to `tasks.human.{role}` queue and send UI event over WS for assigned users.
    3. Execution waits in `paused` state with `awaitingHuman` status.
    4. Human completes decision via UI (approve/deny/extras) — updates `context`.
    5. Orchestrator resumes execution from next step.
* Escalation: if overdue or assignee unavailable, orchestrator can apply fallback rules or escalate to alternate role (director) — this is configurable in blueprint.

### Human vs AI decisioning

* Decision nodes can be declared `policy` nodes:

    * `primary` = AI component; `fallback` = HumanTask when confidence low or human override requested.
* Admin-configurable thresholds and routing rules per blueprint or per organization.

---

## 10. Dynamic workflow modification (per-execution & global)

### Use cases

* Ad-hoc override for a single execution (e.g., manager OOO → route to director).
* Global blueprint update (change rule for all future executions).
* AI-suggested persistent change (detect recurring overrides and prompt admin to persist change).

### Mechanisms

1. **Per-execution patch API**:

    * `PATCH /api/v1/executions/{id}` with `{ operations: [ add/remove/update step, setNextStep, patchContext ] }`
    * Orchestrator applies changes and emits `executionPatch` events.
2. **Admin-guided AI prompt**:

    * Admin issues prompt like “For execution exec-123, if manager is OOO route to director.”
    * Orchestrator translates prompt into `operation` and applies as per-execution update; optionally suggest commit to blueprint.
3. **AI-suggested global change**:

    * Orchestrator tracks override frequency; if override ratio crosses threshold, AI generates a proposed blueprint delta.
    * Admin reviews & approves; if accepted, new blueprint version is created and published.
4. **Atomicity & safety**:

    * Per-execution patches are logged (who/what made change) and reversible (store reverse patch).
    * Patches must conform to validation rules (no orphaned steps, valid references).

### Audit and approvals

* All dynamic changes require audit entries (user/AI id, reason, timestamp, diffs).
* Optionally require two-person approval for global changes (configurable).

---

## 11. Message formats & channels (JSON schemas)

### Common metadata in messages

* `executionId`, `blueprintId`, `blueprintVersion`
* `stepId`, `taskId`, `requestId`
* `nodeId` (invoked node)
* `timestamp`, `ttl`, `correlationId`

### Example control messages (JSON)

* `register` (HTTP POST): includes node metadata + node_public.
* `ws:invoke` (orchestrator → node): `{ type: "invoke", executionId, stepId, inputs, replyTo }`
* `ws:invokeResult` (node → orchestrator): `{ type: "result", executionId, stepId, outputs, error, duration }`
* `ws:heartbeat`: `{ type: "heartbeat", nodeId, metrics }`
* `mq:task` (brokered): headers include `executionId`, `stepId`, body = `{ inputs }`
* `mq:taskResult`: similar to `ws:invokeResult`, published to reply-to queue.

(Implementers should provide exact JSON schemas in `spec/schemas` for validation in the repo.)

---

## 12. Error handling, retries, compensation patterns

### Task failures

* On node error, orchestrator supports:

    * Immediate retry with exponential backoff (config per task).
    * Switch to fallback component (blueprint-defined).
    * Escalate to human task for manual remediation.

### Compensation

* Blueprints may declare compensation steps for tasks that must be undone on failure (e.g., refund).
* Compensation is a first-class step type invoked on execution failure or explicit cancellation.

### Exactly-once vs at-least-once

* Use correlation and idempotency keys to make component handlers idempotent.
* When using MQ, ensure messages carry `idempotencyKey` and design consumers to tolerate duplicates.

---

## 13. Observability, audit, and security considerations

### Observability

* Emit structured logs for every significant event: registration, heartbeat, task-dispatch, task-complete, errors, patches.
* Metrics: Prometheus counters/gauges for active executions, queue depth, node health, latencies.
* Traces: optionally instrument with OpenTelemetry to correlate orchestration traces across components.

### Audit & Compliance

* Store immutable log of:

    * Blueprint versions and diffs
    * Execution events
    * Dynamic patches and approvals
    * AI prompts and responses (subject to PII policy)
* Support export of audit trails per execution.

### Security

* Encrypt sensitive fields in context at rest.
* Use mTLS between sensitive services if possible.
* Implement RBAC for admin APIs; require multi-factor or approval flows for global blueprint changes.

---

## 14. Scaling, HA, and deployment notes

### Master

* Master is the control plane — run it clustered with leader election or stateless nodes backed by shared MongoDB and external distributed lock (Redis / etcd) for leadership.
* WebSocket scaling: use external reverse proxy/load balancer supporting WebSockets and sticky sessions (or place all WS connections through a message broker gateway).
* For high concurrency, scale blueprint runtimes horizontally; orchestrator dispatches tasks to runtimes or components.

### MQ scaling

* RabbitMQ: use mirrored queues or federation for cross-datacenter; use vhosts to separate tenants.
* NATS JetStream or Kafka for very large event volumes.

### Data

* Shard MongoDB for very large registries/execution histories; keep hot execution state in memory + persisted for crash recovery.

---

## 15. Backwards compatibility & versioning strategy

* Blueprints are versioned; runtime knows which version a running execution uses.
* Message schemas versioned by `schemaVersion` header. Orchestrator supports translation layers for older schema versions.
* Component API versioning: `Accept-Version` header on invoke; registry tracks component `apiVersion`.

---

## 16. Future extensions & optional integrations

* **Component Marketplace**: host, rate, and sign community components; allow discovery and installation.
* **Policy engine** (OPA): define access and governance rules for dynamic changes.
* **Local runner**: dev tool to run blueprints in process without deployment (fast feedback loop).
* **Process mining integration**: ingest logs to propose blueprint refinements.
* **On-prem enterprise features**: SSO, RBAC, encryption key management, audit exports.

---

## Appendix: Operational examples (conceptual)

* **Node reconnect scenario**: node reconnects after outage; WS handshake validated via refreshed token; master reassigns queued tasks or instructs node to consume from MQ reply-to queue; orchestrator marks tasks in-flight or requeues if timeout exceeded.
* **Manager OOO flow**: at decision node, orchestrator calls `AiDecision` to check calendar/availability; AI returns `manager_unavailable=true`; orchestrator applies per-execution patch that updates next step to `directorApproval` and logs the patch; execution continues without human blocking.

---

## Closing notes

This SPEC is intended to be a living document. Implementers should:

* Align message schema files to the spec folder in repo.
* Provide conformance tests for registration and WS handshake flows.
* Publish integration examples: a minimal component, a blueprint, and an orchestrator dev setup (docker-compose) illustrating WS-only mode and WS+MQ hybrid mode.

If you want, I can now:

* Produce a `spec/schemas/*.json` bundle for the message payloads.
* Generate a minimal `docker-compose` that starts orchestrator (dev), MongoDB, one sample component, and RabbitMQ in optional mode.
