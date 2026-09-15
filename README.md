# 🏦 Horizon Trust

<p align="center">
  <strong>Event-Driven Digital Banking Platform</strong>
</p>

<p align="center">
  A distributed banking system built with Spring Boot Microservices, Apache Kafka, Redis, Saga Pattern, MySQL, and Spring Cloud Gateway.
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Event--Driven-231F20?logo=apachekafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Saga Pattern](https://img.shields.io/badge/Saga%20Pattern-6A5ACD?logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Microservices](https://img.shields.io/badge/Microservices-0078D4?logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)

</p>

---

## 📌 Overview

**Horizon Trust** is a distributed digital banking platform designed using a **microservices architecture** and **event-driven communication**.

The system models a banking transaction as a distributed workflow involving account management, fraud detection, OTP verification, payment processing, notifications, and transaction completion.

The project uses the **Saga Pattern** to coordinate distributed transactions and execute compensating actions when a downstream operation fails.

Instead of relying on a single database transaction across all services, each service manages its own local transaction while Kafka events and compensation workflows maintain business consistency.

---

## 🚀 Key Features

- 🧩 Independent Spring Boot microservices
- 🔄 Saga Pattern for distributed transaction management
- ♻️ Compensating transactions and refund workflows
- ⚡ Apache Kafka for event-driven communication
- 🔗 OpenFeign for synchronous service-to-service communication
- 🔐 JWT-based authentication
- 🛡️ Multi-rule fraud detection
- 📊 Redis-backed transaction behavior tracking
- 🔑 OTP verification for suspicious transactions
- 💰 Account debit and credit operations
- 🚫 Automatic account blocking after fraud detection
- 💳 Payment order creation
- 🔔 Payment webhook success/failure handling
- 📩 Event-driven notifications
- 🌐 Spring Cloud Gateway
- 🚦 Redis-backed API rate limiting
- 🗄️ MySQL persistence
- 🐳 Docker Compose infrastructure
- ❤️ Spring Boot Actuator health monitoring
- 📖 Swagger / OpenAPI documentation

---

# 🏗️ System Architecture

```text
                                  ┌────────────────┐
                                  │     Client     │
                                  └───────┬────────┘
                                          │
                                          ▼
                               ┌────────────────────┐
                               │    API Gateway     │
                               │                    │
                               │ Spring Cloud       │
                               │ Gateway             │
                               │                    │
                               │ Routing             │
                               │ Redis Rate Limit    │
                               └─────────┬──────────┘
                                         │
             ┌───────────────────────────┼───────────────────────────┐
             │                           │                           │
             ▼                           ▼                           ▼
     ┌────────────────┐        ┌──────────────────┐        ┌────────────────┐
     │ Account        │        │ Transaction      │        │ Payment        │
     │ Service        │        │ Service          │        │ Service        │
     │                │        │                  │        │                │
     │ Account        │        │ Transaction      │        │ Payment Orders  │
     │ Management     │        │ Lifecycle        │        │ Webhooks        │
     │ Debit/Credit   │        │ Saga             │        │ Payment Status  │
     │ Blocking       │        │ Compensation     │        │                │
     └───────┬────────┘        └────────┬─────────┘        └───────┬────────┘
             │                          │                          │
             │                          │                          │
             │                          ▼                          │
             │                    ┌─────────────┐                  │
             │                    │    Kafka    │◄─────────────────┘
             │                    │             │
             │                    │  Event Bus  │
             │                    └──────┬──────┘
             │                           │
             │              ┌────────────┼─────────────┐
             │              │            │             │
             ▼              ▼            ▼             ▼
       ┌──────────┐  ┌──────────────┐ ┌──────────┐ ┌──────────────┐
       │  Redis   │  │    Fraud     │ │ Payment  │ │ Notification │
       │          │  │  Detection   │ │  Events  │ │   Service    │
       │ Fraud    │  │   Service    │ │          │ │              │
       │ State    │  │              │ │          │ │ OTP /        │
       │          │  │ Fraud Rules  │ │          │ │ Notifications│
       │ Rate     │  │ OTP          │ │          │ │              │
       │ Limiting │  │ Verification │ │          │ │              │
       └──────────┘  └──────────────┘ └──────────┘ └──────────────┘

                         ┌─────────────────────┐
                         │        MySQL        │
                         │                     │
                         │ Persistent Data     │
                         └─────────────────────┘
```

---

# 🧩 Microservices

## 👤 Account Service

The Account Service owns the account domain and manages banking account operations.

### Responsibilities

- Account creation
- Unique account number generation
- Account information management
- Account balance management
- Debit balance
- Credit balance
- Account status validation
- Account blocking
- Account-related event processing

The service owns account business logic rather than allowing other services to directly manipulate account data.

---

## 💸 Transaction Service

The Transaction Service coordinates the transaction lifecycle and acts as the primary Saga workflow coordinator.

### Responsibilities

- Initiate transactions
- Validate transaction requests
- Maintain transaction state
- Coordinate account operations
- Trigger fraud detection
- Coordinate payment processing
- Handle transaction completion
- Handle transaction failure
- Execute Saga compensation
- Refund failed transactions
- Publish transaction events
- Maintain transaction history

---

## 🛡️ Fraud Detection Service

The Fraud Detection Service evaluates transactions against multiple fraud detection rules before allowing the transaction workflow to proceed.

### Responsibilities

- Transaction fraud checks
- Suspicious transaction detection
- Transaction amount behavior analysis
- Redis-backed fraud state
- OTP verification workflow
- Fraud-related Kafka events
- Account blocking workflow

### Redis-Based Transaction Analysis

Redis stores frequently accessed fraud-related state for individual accounts.

Example:

```text
fraud:avg_amount:ACC1001 → 2500.00
```

When a new transaction is received, the amount can be compared against the account's historical transaction behavior.

This provides fast access to fraud-related state without repeatedly querying the primary database.

---

# 🔑 OTP Verification

Suspicious transactions can require additional OTP verification before continuing.

```text
                    Transaction
                         │
                         ▼
                 Fraud Detection
                         │
                         ▼
                   Fraud Check
                    /       \
                   /         \
               Normal       Suspicious
                 │              │
                 ▼              ▼
              Continue       Generate OTP
                                │
                                ▼
                         Notification Service
                                │
                                ▼
                           User enters OTP
                                │
                           ┌────┴────┐
                           │         │
                         Valid     Invalid
                           │         │
                           ▼         ▼
                       Continue   Compensation
                                     │
                              ┌──────┴──────┐
                              ▼             ▼
                           Refund       Block Account
```

OTP verification provides an additional security layer for transactions identified as suspicious.

---

# 💳 Payment Service

The Payment Service manages payment processing within the transaction workflow.

### Responsibilities

- Create payment orders
- Process payment requests
- Handle payment results
- Process payment webhooks
- Handle successful payments
- Handle failed payments
- Publish payment events
- Communicate payment status to the transaction workflow

### Payment Flow

```text
Transaction Service
        │
        ▼
Payment Service
        │
        ▼
Create Payment Order
        │
        ▼
Payment Gateway
        │
        ▼
Payment Processing
        │
        ▼
Webhook
        │
        ├───────────────┐
        ▼               ▼
     SUCCESS          FAILURE
        │               │
        ▼               ▼
 Complete Saga       Compensation
                         │
                         ▼
                       Refund
```

Webhook processing allows the system to react to the final payment result asynchronously.

---

# 📩 Notification Service

The Notification Service handles asynchronous user notifications generated by banking workflows.

### Responsibilities

- Consume Kafka events
- Send transaction notifications
- Send OTP notifications
- Notify users about transaction status
- Process asynchronous notification workflows

The service is decoupled from the transaction workflow through Kafka events.

---

# 🌐 API Gateway

Horizon Trust uses **Spring Cloud Gateway** as the single entry point for client requests.

### Responsibilities

- Centralized API routing
- Service endpoint abstraction
- Redis-backed request rate limiting
- Gateway-level traffic control

The Gateway does **not** contain custom JWT authentication logic.

Example:

```text
                         API Gateway
                              │
          ┌───────────────────┼────────────────────┐
          │                   │                    │
          ▼                   ▼                    ▼
 /api/v1/account/**   /api/v1/transaction/**  /api/v1/payment/**
          │                   │                    │
          ▼                   ▼                    ▼
   Account Service     Transaction Service    Payment Service
```

Clients communicate through the Gateway without needing to know the internal service addresses.

---

# 🚦 Redis Rate Limiting

The API Gateway uses Redis-backed `RequestRateLimiter` to protect APIs from excessive traffic.

Example configuration:

```text
Replenish Rate : 10 requests/second
Burst Capacity : 20 requests
```

### Request Flow

```text
Client
  │
  ▼
API Gateway
  │
  ▼
Redis Rate Limiter
  │
  ├──────────────► Request Allowed
  │                       │
  │                       ▼
  │                  Backend Service
  │
  └──────────────► Limit Exceeded
                          │
                          ▼
                    HTTP 429
                Too Many Requests
```

Redis provides the shared state required by the rate-limiting mechanism.

---

# ⚡ Event-Driven Architecture

Apache Kafka is used as the event backbone for asynchronous communication between services.

Services publish business events to Kafka, allowing other services to consume and react to those events independently.

```text
Producer Service
       │
       │ Publish Event
       ▼
     Kafka
       │
       │ Consume Event
       ▼
Consumer Service
```

### Example: Fraud Detection

When fraud is detected:

```text
Fraud Detection Service
          │
          │ FRAUD_DETECTED_EVENT
          ▼
        Kafka
          │
          ▼
   Account Service
          │
          ▼
     Block Account
```

This keeps account-blocking logic inside the Account Service while keeping the Fraud Detection Service decoupled.

---

# 📡 Kafka Concepts Used

Horizon Trust uses Apache Kafka for event-driven communication.

### Producer

A service that publishes an event to Kafka.

### Topic

A named stream/category where events are published.

### Partition

A topic can contain multiple partitions. Kafka maintains ordering within a partition and uses partitions to scale event processing.

### Consumer

A service that reads and processes Kafka events.

### Consumer Group

Multiple consumers in the same group share the processing workload.

Different consumer groups can independently consume the same Kafka topic.

### Offset

Kafka assigns each record an offset representing its position within a partition.

```text
Topic
 │
 ├── Partition 0
 │      ├── Offset 0
 │      ├── Offset 1
 │      └── Offset 2
 │
 └── Partition 1
        ├── Offset 0
        ├── Offset 1
        └── Offset 2
```

---

# 🔄 Saga Pattern

Horizon Trust uses the **Saga Pattern** to coordinate distributed transactions across multiple microservices.

A banking transaction may involve multiple independent local transactions:

```text
Transaction Started
        │
        ▼
Account Service
        │
        │ Debit
        ▼
Fraud Detection
        │
        ▼
Payment Service
        │
        ▼
Transaction Completed
```

Each service manages its own local transaction.

The system does not depend on a single global database transaction spanning all microservices.

---

# ♻️ Saga Compensation

When a downstream operation fails, previously completed operations can be compensated.

## Payment Failure

```text
                  Transaction Started
                          │
                          ▼
                    Debit Account
                          │
                          ▼
                    Fraud Check
                          │
                          ▼
                       Payment
                          │
                          X
                       FAILED
                          │
                          ▼
                    Compensation
                          │
                          ▼
                    Credit Account
                          │
                          ▼
                 Transaction REFUNDED
```

## Fraud Compensation

When fraud is detected:

```text
                  Fraud Detected
                        │
             ┌──────────┴──────────┐
             │                     │
             ▼                     ▼
        Refund Amount        FRAUD_DETECTED_EVENT
             │                     │
             ▼                     ▼
      Transaction Service    Account Service
                                   │
                                   ▼
                             Block Account
```

The Transaction Service performs the financial compensation while the Account Service owns account-blocking logic.

---

# 🔗 Inter-Service Communication

Horizon Trust uses both synchronous and asynchronous communication.

## OpenFeign — Synchronous Communication

OpenFeign is used when a service requires an immediate response from another service.

```text
Transaction Service
       │
       │ OpenFeign
       ▼
Account Service
       │
       ▼
Immediate Response
```

This is useful for operations where the transaction workflow needs the result before proceeding.

---

## Apache Kafka — Asynchronous Communication

Kafka is used for business events and decoupled workflows.

```text
Service A
   │
   │ Business Event
   ▼
 Kafka
   │
   ▼
Service B
```

This allows consumers to process events independently from the producer.

---

# 🔐 Security

Horizon Trust uses **JWT-based authentication** to secure protected APIs.

Authentication and authorization are kept separate from the Gateway's primary responsibilities of routing and rate limiting.

```text
Client
  │
  │ JWT
  ▼
Protected API
  │
  ▼
Authenticated Request
```

---

# 🗄️ Data Architecture

Horizon Trust uses MySQL for persistent application data and Redis for high-speed, frequently accessed state.

```text
                 Horizon Trust
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
        MySQL                   Redis
          │                       │
          │                       ├── Fraud State
          │                       │
          │                       └── Rate Limiting
          │
          └── Persistent Data
```

---

# 📖 API Documentation

Swagger / OpenAPI documentation is implemented for the backend APIs.

This makes it easier to:

- Explore available endpoints
- Understand request and response models
- Test APIs during development
- Document the service contracts

Typical Springdoc Swagger UI endpoint:

```text
/swagger-ui/index.html
```

---

# 🛠️ Technology Stack

## Backend

- Java 17+
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Validation
- Spring Security
- JWT

## Microservices & Communication

- Spring Cloud Gateway
- OpenFeign
- Apache Kafka
- Saga Pattern

## Data & Infrastructure

- MySQL
- Redis
- Docker
- Docker Compose

## Development & Testing

- Maven
- Git
- GitHub
- Postman
- Swagger / OpenAPI
- Spring Boot Actuator

---

# 📁 Project Structure

```text
Horizon-Trust/
│
├── account-service/
│   └── src/
│
├── transaction-service/
│   └── src/
│
├── fraud-detection-service/
│   └── src/
│
├── payment-service/
│   └── src/
│
├── notification-service/
│   └── src/
│
├── api-gateway/
│   └── src/
│
├── docker-compose.yml
│
├── pom.xml
│
└── README.md
```

Each microservice contains its own API, business logic, persistence and configuration.

---

# 🔌 API Gateway Routes

The API Gateway provides a unified entry point for backend APIs.

```text
/api/v1/account/**
        │
        └──► Account Service

/api/v1/transaction/**
        │
        └──► Transaction Service

/api/v1/fraud/**
        │
        └──► Fraud Detection Service

/api/v1/payment/**
        │
        └──► Payment Service

/api/v1/notification/**
        │
        └──► Notification Service
```

Clients communicate through the Gateway without needing to know the internal service addresses.

---

# 🧪 Testing

The application can be tested using **Postman** and Swagger/OpenAPI.

## Standard Transaction Workflow

```text
1. Authenticate
       │
       ▼
2. Create / Access Account
       │
       ▼
3. Initiate Transaction
       │
       ▼
4. Debit Account
       │
       ▼
5. Fraud Detection
       │
       ├────────────────────┐
       │                    │
       ▼                    ▼
    Normal              Suspicious
       │                    │
       │                    ▼
       │              OTP Verification
       │                    │
       │              ┌─────┴─────┐
       │              ▼           ▼
       │            Valid       Invalid
       │              │           │
       │              ▼           ▼
       │           Continue    Compensation
       │                           │
       │                           ├──► Refund
       │                           │
       │                           └──► Block Account
       │
       ▼
6. Payment Order
       │
       ▼
7. Payment Gateway
       │
       ▼
8. Webhook
       │
       ├── Success ─────► Complete Transaction
       │
       └── Failure ─────► Saga Compensation
```

---

# ❌ Failure Handling

Distributed systems must account for partial failures.

Horizon Trust implements compensation workflows for important failure scenarios.

## Payment Failure

```text
Account Debited
      │
      ▼
Fraud Check Passed
      │
      ▼
Payment Failed
      │
      ▼
Saga Compensation
      │
      ▼
Credit Account
      │
      ▼
Transaction REFUNDED
```

## Fraud Detection

```text
Transaction
      │
      ▼
Fraud Detection
      │
      ▼
Fraud Detected
      │
      ├─────────────► Refund
      │
      └─────────────► Block Account
```

## Invalid OTP

```text
Suspicious Transaction
        │
        ▼
      OTP
        │
        ▼
    Invalid OTP
        │
        ▼
    Compensation
        │
        ├──► Refund
        │
        └──► Block Account
```

---

# ❤️ Why Saga Instead of a Distributed Database Transaction?

In a microservices architecture, each service owns its own data and local transactions.

For example:

```text
Account Service
      │
      └── Account Data

Transaction Service
      │
      └── Transaction Data

Payment Service
      │
      └── Payment Data
```

A single database transaction cannot easily span these independent services.

The Saga Pattern handles this by coordinating local transactions and executing compensating operations when a later step fails.

```text
Local Transaction
       ↓
Local Transaction
       ↓
Local Transaction
       ↓
Failure
       ↓
Compensating Transaction
```

---

# 📊 Health Monitoring

Spring Boot Actuator is used to expose service health information.

Example:

```http
GET /actuator/health
```

Example response:

```json
{
  "status": "UP"
}
```

This can be used to verify service availability during local development and deployment.

---

# 🐳 Docker

Docker Compose simplifies the setup of local infrastructure dependencies.

### Start infrastructure

```bash
docker compose up -d
```

### Stop infrastructure

```bash
docker compose down
```

### Check running containers

```bash
docker compose ps
```

---

# ⚙️ Local Development Setup

## Prerequisites

- Java 17+
- Maven
- MySQL
- Redis
- Apache Kafka
- Docker
- Docker Compose
- Git

---

## 1. Clone Repository

```bash
git clone https://github.com/Ignorantashwin/Horizon-Trust.git

cd Horizon-Trust
```

---

## 2. Start Infrastructure

```bash
docker compose up -d
```

---

## 3. Configure Services

Each microservice uses its own local configuration.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/horizonTrust
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.kafka.bootstrap-servers=localhost:9092

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

> Local configuration files containing credentials are excluded from version control.

---

## 4. Run a Service

Example:

```bash
cd account-service
./mvnw spring-boot:run
```

Start the remaining services from their respective directories.

---

# 🔒 Configuration & Secrets

Sensitive configuration is intentionally kept outside version control.

The repository ignores local configuration files such as:

```text
application.properties
application-local.properties
application-local.yml
.env
.env.*
```

This prevents database credentials, API keys and environment-specific configuration from being accidentally committed.

---

# 🌍 Deployment

Horizon Trust is designed to run as independently deployable microservices.

Production-style deployment:

```text
                         Internet
                            │
                            ▼
                   ┌─────────────────┐
                   │   API Gateway   │
                   │   Public URL    │
                   └────────┬────────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
              ▼             ▼             ▼
          Account      Transaction     Payment
          Service        Service       Service
              │             │             │
              └─────────────┼─────────────┘
                            │
                    ┌───────┴───────┐
                    ▼               ▼
                  Kafka           Redis
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
        Fraud   Notification  Events
```

---

# 🌐 Live Demo

### API Gateway

**Live URL:** `YOUR_LIVE_GATEWAY_URL`

### Health Check

**Health:** `YOUR_LIVE_GATEWAY_URL/actuator/health`

### Swagger / OpenAPI

**Swagger URL:** `YOUR_SWAGGER_URL`

> The application is deployed using free-tier infrastructure. Services may take some time to respond after periods of inactivity depending on the hosting provider.

---

# 📌 Engineering Highlights

### Distributed Transaction Management

Implemented Saga-based transaction workflows with compensating actions for failed downstream operations.

### Event-Driven Microservices

Implemented Apache Kafka for asynchronous communication between independent services.

### Fraud Detection

Implemented multiple fraud validation rules with Redis-backed transaction behavior tracking.

### OTP-Based Verification

Added OTP verification for suspicious transactions before allowing the workflow to continue.

### Compensation & Refund

Implemented compensation workflows that restore the account balance and update transaction state when a transaction cannot be completed.

### Event-Driven Account Blocking

Published fraud events through Kafka and allowed the Account Service to independently process account-blocking logic.

### Payment Webhooks

Implemented payment order creation and webhook-based success/failure processing.

### Redis API Rate Limiting

Implemented Redis-backed request rate limiting at the API Gateway to protect backend endpoints from excessive traffic.

### Hybrid Service Communication

Used OpenFeign for synchronous service communication and Kafka for asynchronous business events.

### Service Isolation

Separated account, transaction, fraud, payment and notification responsibilities into independent microservices.

---

# 🧠 Engineering Concepts Demonstrated

```text
Microservices
│
├── Service Boundaries
├── Independent Business Logic
└── Service Isolation

Event-Driven Architecture
│
├── Kafka Producers
├── Kafka Consumers
├── Topics
├── Partitions
├── Consumer Groups
└── Asynchronous Processing

Distributed Transactions
│
├── Saga Pattern
├── Local Transactions
├── Compensation
└── Failure Recovery

Security
│
├── JWT Authentication
└── OTP Verification

Performance & Protection
│
├── Redis
├── Fraud State
└── API Rate Limiting

Service Communication
│
├── OpenFeign
└── Apache Kafka

Payments
│
├── Order Creation
├── Webhooks
└── Failure Compensation
```

---

# 🔮 Future Improvements

- Distributed tracing with OpenTelemetry
- Centralized logging
- Prometheus and Grafana monitoring
- Kafka retry topics
- Dead Letter Topics
- Strongly typed event schemas
- Transactional Outbox Pattern
- Automated integration testing
- Contract testing between services
- CI/CD pipeline
- Kubernetes deployment
- Production-grade secrets management
- Cloud-native service discovery

---

# 📚 Project Motivation

Horizon Trust was built to explore practical backend engineering challenges involved in distributed financial systems.

The project focuses on designing workflows that can behave predictably when individual services fail.

Key areas explored:

- Microservice architecture
- Distributed transactions
- Saga orchestration
- Event-driven architecture
- Kafka producers and consumers
- Kafka topics and consumer groups
- Redis-based state management
- Synchronous and asynchronous communication
- Fraud detection
- OTP verification
- Payment processing
- Webhook handling
- Compensation workflows
- API rate limiting
- JWT authentication
- Service failure handling
- Dockerized infrastructure

---

# 🎯 What This Project Demonstrates

This project demonstrates practical backend engineering concepts expected from a modern Java Backend Developer:

```text
Java
  ↓
Spring Boot
  ↓
REST APIs
  ↓
Spring Data JPA
  ↓
MySQL
  ↓
Microservices
  ↓
OpenFeign
  ↓
Apache Kafka
  ↓
Event-Driven Architecture
  ↓
Saga Pattern
  ↓
Redis
  ↓
Fraud Detection
  ↓
API Rate Limiting
  ↓
JWT + OTP Security
  ↓
Docker
```

---

# 👨‍💻 Author

## Ashwin

**Java Backend Developer**

Java • Spring Boot • Microservices • Apache Kafka • Redis • MySQL

### GitHub

https://github.com/Ignorantashwin

### LinkedIn

Add your LinkedIn profile here.

---

## ⭐ Support

If you find this project interesting, consider giving the repository a ⭐.
