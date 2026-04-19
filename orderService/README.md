# Order Service (Kafka Producer)

The **orderService** is a Spring Boot microservice that accepts order requests via REST API, persists them to an H2 database, and publishes `OrderEvent` messages to a Kafka topic for downstream consumers.

## Responsibilities

1. Accept order creation requests via REST API
2. Save order to `orders_info` table
3. Track processing in `processed_orders` table
4. Publish `OrderEvent` to Kafka topic `order-events`

## Running

```bash
./mvnw spring-boot:run
```

Starts on **port 8085**.

## API

### POST `/app/publish`

Creates an order and publishes it to Kafka.

**Request Body:**
```json
{
  "products": ["Laptop", "Mouse"],
  "amount": 1299.99,
  "orderProcessingDateTime": "2026-04-19T10:30:00.000Z",
  "customerEmail": "customer@example.com"
}
```

**Success Response (200):**
```json
{
  "orderId": 3,
  "message": "Successfully published message"
}
```

**Failure Response (500):**
```json
{
  "orderId": -1,
  "message": "Failed to published message"
}
```

## Key Classes

| Class | Role |
|---|---|
| `OrdersController` | REST endpoint — receives orders |
| `OrderProcessor` / `OrderProcessorImpl` | Business logic — saves to DB, builds event, calls producer |
| `OrderProducer` | Kafka producer — sends `OrderEvent` via `KafkaTemplate` with async callback |
| `OrderEvent` | Record — message payload (id, products, amount, paymentDateTime, customerEmail) |
| `Orders` | JPA entity — persisted order data |
| `ProcessedOrders` | JPA entity — tracks which orders have been published |
| `OrderRequestDTO` / `OrderResponseDTO` | API request/response shapes |

## Kafka Producer Configuration

Configured in `application.yaml`:

| Property | Value | Purpose |
|---|---|---|
| `key-serializer` | `LongSerializer` | Order ID as partition key |
| `value-serializer` | `JsonSerializer` | OrderEvent serialized as JSON |
| `acks` | `all` | Wait for all in-sync replicas |
| `enable-idempotence` | `true` | Exactly-once producer semantics |
| `retries` | `3` | Retry on transient failures |
| `linger.ms` | `10` | Wait 10ms to batch messages |
| `batch.size` | `16384` | 16KB batch size |
| `spring.json.add.type.headers` | `false` | Omit type headers for cross-package deserialization |

## Database

Uses H2 in-memory (`jdbc:h2:mem:test`). Tables auto-created via `ddl-auto: create-drop`.

- **orders_info** — stores all orders
- **processed_orders** — tracks orders that were published to Kafka

H2 Console: [http://localhost:8085/h2](http://localhost:8085/h2)

## Seed Data

`OrderServiceApplication.init()` populates 2 sample orders on startup via `@PostConstruct`.
