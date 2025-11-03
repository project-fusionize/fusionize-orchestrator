# Fusionize

**Distributed, AI-Native Business Process Management Engine**

Fusionize is an open-source, event-driven BPM (Business Process Management) system designed for scalability, reliability, and native AI integration. It orchestrates distributed workflows where **AI and humans collaborate** through a unified event-driven architecture.

---

## 🚀 Core Architecture

Fusionize operates as a **distributed orchestrator** coordinating a network of **workflow nodes**.
Each workflow is defined declaratively using YAML or as a **Kubernetes Custom Resource (CRD)**.
Nodes represent tasks or decision points that can be executed by humans, AI models, or system components.
Communication between orchestrators and nodes is **event-driven** and uses **message queues (AMQP)** for scalability and fault tolerance.

**Key architectural components:**

* **Orchestrator:** Central runtime managing workflow execution, node state, event routing, and retries.
* **Node:** A modular unit of execution (human, AI, or system).
* **Events:** All workflow transitions are driven by events published and consumed asynchronously.
* **Health Checks & Sync:** Real-time orchestration-state synchronization uses WebSocket channels.

Fusionize is built for **horizontal scalability** (multi-instance orchestration), **resilience**, and **observability**. Each workflow instance is event-sourced, allowing replay and auditability.

---

## 🧩 Node Types

Fusionize supports a variety of node types for hybrid AI-human workflows:

| **Node Type** | **Purpose**                                                                        | **Execution Source**                                                | **Behavior / Description**                                                                                                                                                               | **Example Use Case**                                                                                                                                                                                       |
| ------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **START**     | Entry point of a workflow. Defines the trigger condition that initiates execution. | Event-based trigger (e.g., file upload, message received, API call) | Listens for a domain or system event, then starts the workflow instance and routes execution to the next node(s).                                                                        | `start:document.fileUploaded` — starts workflow when a file is uploaded to a storage bucket.                                                                                                               |
| **END**       | Terminates a workflow or branch. Marks completion.                                 | None (terminal node)                                                | Represents a final state of the process (success, failure, rejection, completion). No further transitions occur.                                                                         | `end:document.approved` or `end:document.rejected`                                                                                                                                                         |
| **DECISION**  | Chooses the next path based on evaluation of data or logic.                        | **AI**, **Human**, or **System**                                    | Evaluates conditions or prompts to determine which branch to follow. AI and Human variants can involve reasoning or manual selection, while System is rule-based.                        | - *AI Decision*: LLM determines if a document passes quality criteria. <br> - *Human Decision*: User approves or rejects a submission. <br> - *System Decision*: Validates checksum or metadata integrity. |
| **TASK**      | Executes an action or work step.                                                   | **AI**, **Human**, or **System**                                    | Performs a defined operation and emits an event upon completion. AI tasks use LLMs; Human tasks involve form input or approval; System tasks perform logic, integration, or computation. | - *System Task*: Extract document text with Apache Tika. <br> - *AI Task*: Summarize a document. <br> - *Human Task*: Add notes to rejected documents.                                                     |
| **WAIT**      | Suspends workflow execution until a specified trigger or condition occurs.         | Event or timer trigger                                              | Pauses progression at this node until a defined event, time window, or condition is met. Useful for asynchronous processes, callbacks, or human responses.                               | Wait for external system acknowledgment, file readiness, or a user confirmation before continuing.                                                                                                         |


### ⚙️ Components

* **Runtime units** that execute the logic behind each workflow node.
* **Distributed across the cluster** and **invoked asynchronously** by orchestrators via event-driven messaging.
* Include **built-in components** like the **script runner** for Kotlin, Groovy, and JavaScript execution.
* Support **custom components** through a **Spring Boot Starter**, enabling microservice-based extensions.
* Exposed as **MCP (Model Context Protocol) services**, allowing AI nodes to **perform tool calls** for intelligent, adaptive orchestration.


## Example Workflow (YAML)

```yaml
kind: Workflow
apiVersion: v1
name: Document Ingestion and Review Workflow
domain: document.workflow
key: document-ingestion-workflow
description: Handles file ingestion, AI summarization, validation, and human review/approval
version: 1
active: true

nodes:
  start:
    type: START
    component: start:document.fileUploaded
    componentConfig:
      bucket: uploads
      pattern: "*.pdf"
    next: [verifyFile]

  verifyFile:
    type: DECISION
    component: system:decision.verifyFileIntegrity
    componentConfig:
      conditions:
        valid: extractContent
        invalid: endReject
    next: [extractContent, endReject]

  extractContent:
    type: TASK
    component: system:task.extractDocumentContent
    componentConfig:
      parser: "tika"
      outputFormat: "text"
    next: [aiSummarize]

  aiSummarize:
    type: TASK
    component: ai:task.summarizeDocument
    componentConfig:
      model: "gpt-4"
      promptTemplate: "Summarize the content clearly and concisely."
    next: [aiValidateSummary]

  aiValidateSummary:
    type: TASK
    component: ai:task.validateSummary
    componentConfig:
      model: "gpt-4"
      promptTemplate: "Validate that the summary accurately reflects the document."
    next: [aiDecisionApproval]

  aiDecisionApproval:
    type: DECISION
    component: ai:decision.evaluateQuality
    componentConfig:
      promptTemplate: >
        Based on the validation and document summary, decide whether to approve or reject the file.
      routeMap:
        approve: humanFinalApproval
        reject: humanRejectionReview
    next: [humanFinalApproval, humanRejectionReview]

  humanRejectionReview:
    type: TASK
    component: human:task.reviewRejection
    componentConfig:
      formId: "doc-review-reject"
      requiredFields: ["reason", "note"]
    next: [humanOverrideDecision]

  humanOverrideDecision:
    type: DECISION
    component: human:decision.overrideRejection
    componentConfig:
      routeMap:
        override: humanFinalApproval
        confirmReject: endReject
    next: [humanFinalApproval, endReject]

  humanFinalApproval:
    type: DECISION
    component: human:decision.finalApproval
    componentConfig:
      routeMap:
        approve: endApprove
        reject: humanRejectionReview
    next: [endApprove, humanRejectionReview]

  endApprove:
    type: END
    component: end:document.approved
    next: []

  endReject:
    type: END
    component: end:document.rejected
    next: []

```

---

## 🛠️ Tech Stack

| Component                          | Technology                                                            |
| ---------------------------------- | --------------------------------------------------------------------- |
| **Language / Framework**           | Java 21, Spring Boot                                                  |
| **AI Runtime**                     | Spring AI (LLM orchestration, MCP component)                          |
| **Messaging / Queueing**           | RabbitMQ (AMQP)                                                       |
| **Storage**                        | MongoDB (NoSQL + Vector Store for embeddings)                         |
| **Authentication / Authorization** | Keycloak (OpenID Connect)                                             |
| **Communication Security**         | RSA asymmetric encryption (signatures between nodes and orchestrator) |
| **Realtime Updates / Health**      | WebSocket                                                             |
| **Script Engines**                 | Kotlin, Groovy, JavaScript                                            |
| **Workflow Definition**            | YAML parser and Kubernetes CRDs                                       |
| **Extension SDK**                  | Spring Boot Starter for custom component implementation               |

---

## 🧬 Design Principles

* **Event-Driven Execution:** Every workflow transition is triggered by domain or system events, ensuring asynchronous, scalable orchestration.
* **AI-Native:** AI is a first-class participant — not an external plugin. AI can make decisions, perform actions, and trigger human collaboration.
* **Scalable & Reliable:** Built on RabbitMQ for distributed message flow, MongoDB for persistence, and WebSocket for real-time coordination.
* **Secure by Design:** RSA-based communication signatures, Keycloak for identity management, and fine-grained component-level authorization.
* **Extensible:** Developers can add new node types and system tasks using a pluggable Spring Boot starter API.

---

## 📦 Getting Started (Quick Overview)

1. Define your workflow in YAML or as a Kubernetes CRD.
2. Register nodes and components with the orchestrator.
3. Run the orchestrator service (Spring Boot).
4. Deploy distributed node runtimes that subscribe to relevant queues.
5. Observe and interact via WebSocket dashboards or API endpoints.

