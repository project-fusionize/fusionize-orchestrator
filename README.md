# Fusionize

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-ed8b00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Build Status](https://github.com/project-fusionize/fusionize-orchestrator/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/project-fusionize/fusionize-orchestrator/actions/workflows/gradle-build.yml)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6db33f?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)

**Distributed, AI-Native Business Process Management Engine**

Fusionize is an open-source, event-driven BPMS (Business Process Management System) designed for scalability, reliability, and native AI integration. It orchestrates distributed workflows where **AI and humans collaborate** through a unified event-driven architecture.


## 📦 Getting Started (DEVELOPER Guide)
### Prerequisites
- Docker & Docker Compose
- Java 21+

### Installation

1. **Clone the repository:**
   ```bash
   git clone git@github.com:project-fusionize/fusionize-orchestrator.git
   ```

2. **Start Infrastructure:**
   ```bash
   cd fusionize-orchestrator
   docker-compose up -d
   ```
   > **Note:** The first startup may take some time as it pulls images and initializes containers (especially Keycloak and MongoDB).

3. **Start the Application:**
   ```bash
   ./gradlew dev
   ```
   > **Troubleshooting:** If you encounter an authentication error on startup, it is likely because Keycloak is not yet fully ready. Please wait a few moments and restart the application.

### Accessing Services

Once everything is running, you can access the following services:

| Service | URL | Credentials (User/Pass) |
|---------|-----|-------------------------|
| **Fusionize Hub** | [http://localhost:3131](http://localhost:3131) | `fusionize-admin` / `admin` |
| **Keycloak** | [http://localhost:8080](http://localhost:8080) | `admin` / `admin` (System Admin) |
| **RabbitMQ** | [http://localhost:15672](http://localhost:15672) | `fusionize` / `fusionize` |

#### Logging into Fusionize Hub
To log in to the Hub:
1. Go to [http://localhost:3131](http://localhost:3131).
2. You will be redirected to Keycloak.
3. Use the **Realm Admin** credentials:
   - **Username:** `fusionize-admin`
   - **Password:** `admin`

These credentials are configured in `docker/keycloak/realm-export/fuz-realm.json`.
## 🚀 Core Architecture

Fusionize operates as a **distributed orchestrator** coordinating a network of **workflow nodes**.
Each workflow is defined declaratively using YAML or as a **Kubernetes Custom Resource (CRD)**.
Nodes represent tasks or decision points that can be executed by humans, AI models, or system components.
Communication between orchestrators and nodes is **event-driven** and uses **message queues (AMQP)** for scalability and fault tolerance.


## 🧬 Design Principles

* **Event-Driven Execution:** Every workflow transition is triggered by domain or system events, ensuring asynchronous, scalable orchestration.
* **AI-Native:** AI is a first-class participant — not an external plugin. AI can make decisions, perform actions, and trigger human collaboration.
* **Scalable & Reliable:** Built on RabbitMQ for distributed message flow, MongoDB for persistence, and WebSocket for real-time coordination.
* **Secure by Design:** RSA-based communication signatures, Keycloak for identity management, and fine-grained component-level authorization.
* **Extensible:** Developers can add new node types and system tasks using a pluggable Spring Boot starter API.

---

