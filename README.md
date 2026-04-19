# Spring Boot with Apache Kafka — Event-Driven Order & Notification System

A microservices demo built with **Spring Boot 4.0.5**, **Java 21**, and **Apache Kafka** demonstrating an event-driven architecture where an Order Service publishes order events and a Notification Service consumes them to send email confirmations.

## Architecture

```
┌──────────────────┐       Kafka Topic        ┌──────────────────────┐
│   orderService   │  ──── order-events ────▶  │ notificationService  │
│   (Producer)     │                           │    (Consumer)        │
│   Port 8085      │                           │    Port 8086         │
└──────┬───────────┘                           └──────┬───────────────┘
       │                                              │
   H2 Database                                    H2 Database
   (orders_info,                                  (notification)
    processed_orders)                              + Email (SMTP)
```

**Flow:**
1. Client sends POST `/app/publish` to orderService
2. Order is saved to H2 DB and an `OrderEvent` is published to Kafka topic `order-events`
3. notificationService consumes the event, sends an email, and saves notification status to its own DB

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Messaging | Apache Kafka (Confluent 7.6.0) |
| Database | H2 (in-memory) |
| Build | Maven |
| Serialization | Jackson JSON |
| Resilience | Resilience4j (Circuit Breaker) |
| Email | Spring Mail (Gmail SMTP) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Monitoring | Spring Actuator (notificationService) |
| Containerization | Docker Compose |

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

### 1. Start Kafka Infrastructure

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** on port `2181`
- **Kafka Broker** on port `9092`
- **Kafka UI** on port `8888` → [http://localhost:8888](http://localhost:8888)

### 2. Start Order Service (Producer)

```bash
cd orderService
./mvnw spring-boot:run
```

Runs on [http://localhost:8085](http://localhost:8085)

### 3. Start Notification Service (Consumer)

```bash
cd notificationService
./mvnw spring-boot:run
```

Runs on [http://localhost:8086/notification-service](http://localhost:8086/notification-service)

### 4. Publish an Order

```bash
curl -X POST http://localhost:8085/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop", "Mouse"],
    "amount": 1299.99,
    "orderProcessingDateTime": "2026-04-19T10:30:00.000Z",
    "customerEmail": "customer@example.com"
  }'
```

### 5. Check Notification Status

```bash
curl http://localhost:8086/notification-service/api/notifications/all
```

## API Endpoints

### orderService (Port 8085)

| Method | Path | Description |
|---|---|---|
| POST | `/app/publish` | Create order and publish Kafka event |

### notificationService (Port 8086, context: `/notification-service`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications/health` | Health check |
| GET | `/api/notifications/order/{orderId}` | Get notification by order ID |
| GET | `/api/notifications/all` | List all notifications |
| GET | `/api/notifications/status/{status}` | Filter by status (SUCCESS, FAILED, PENDING, RETRYING) |

### Swagger UI

- orderService: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- notificationService: [http://localhost:8086/notification-service/swagger-ui.html](http://localhost:8086/notification-service/swagger-ui.html)

## Kafka Configuration Highlights

### Producer (orderService)
- `acks=all` — waits for all replicas to acknowledge (durability)
- `enable-idempotence=true` — exactly-once semantics at producer level
- `retries=3` — automatic retry on transient failures
- Batching: `linger.ms=10`, `batch.size=16384`

### Consumer (notificationService)
- `auto-offset-reset=earliest` — reads from beginning for new consumer groups
- `enable-auto-commit=false` — manual acknowledgment after successful processing
- `fetch-min-bytes=1`, `fetch-max-wait-ms=5000` — long polling configuration
- `max-poll-records=10` — bounded batch size per poll
- Manual `AckMode.MANUAL` via custom `KafkaConsumerConfig`

## Email Configuration

Set environment variables for Gmail SMTP:

```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

## Project Structure

```
Spring-boot-with-kafka/
├── docker-compose.yml          # Kafka + Zookeeper + Kafka UI
├── orderService/               # Producer microservice
│   └── src/main/java/.../
│       ├── controller/         # REST endpoints
│       ├── dto/                # Request/Response DTOs
│       ├── entity/             # JPA entities (Orders, ProcessedOrders)
│       ├── event/              # OrderEvent record
│       ├── repository/         # Spring Data JPA repos
│       └── service/            # Business logic + KafkaTemplate producer
└── notificationService/        # Consumer microservice
    └── src/main/java/.../
        ├── config/             # KafkaConsumerConfig (manual ack, error handler)
        ├── controller/         # Notification query endpoints
        ├── dao/                # NotificationRepository
        ├── dto/                # StatusMessage enum
        ├── entity/             # Notification JPA entity
        ├── event/              # OrderEvent record (consumer copy)
        ├── kafkaConsumer/      # @KafkaListener consumer
        ├── service/            # Email sending + DB persistence
        └── util/               # EmailUtils (masking)
```

## Shutting Down

```bash
docker-compose down
```
