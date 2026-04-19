# Notification Service (Kafka Consumer)

The **notificationService** is a Spring Boot microservice that consumes `OrderEvent` messages from Kafka, sends email notifications to customers via Gmail SMTP, and persists notification status to an H2 database.

## Responsibilities

1. Consume `OrderEvent` messages from Kafka topic `order-events`
2. Send email confirmation to customer
3. Save notification status (SUCCESS / FAILED) to database
4. Expose REST API to query notification status

## Running

```bash
./mvnw spring-boot:run
```

Starts on **port 8086** with context path `/notification-service`.

## API

### GET `/api/notifications/health`
Health check.

### GET `/api/notifications/order/{orderId}`
Get notification status for a specific order.

### GET `/api/notifications/all`
List all notifications.

### GET `/api/notifications/status/{status}`
Filter notifications by status: `SUCCESS`, `FAILED`, `PENDING`, `RETRYING`.

## Key Classes

| Class | Role |
|---|---|
| `OrderEventConsumer` | `@KafkaListener` with `@RetryableTopic` — consumes from `order-events` with automatic retry + DLT |
| `NotificationService` / `NotificationServiceImpl` | Sends email via `JavaMailSender`, saves notification to DB |
| `KafkaConsumerConfig` | Custom consumer factory with `JacksonJsonDeserializer`, `setConcurrency(3)`, `read_committed` isolation |
| `NotificationController` | REST endpoints to query notification status |
| `Notification` | JPA entity — tracks notification outcome per order |
| `NotificationRepository` | Spring Data JPA — find by orderId, check existence |
| `StatusMessage` | Enum — SUCCESS, FAILED, PENDING, RETRYING |
| `EmailUtils` | Utility — masks email addresses in logs for privacy |
| `OrderEvent` | Record — deserialized Kafka message payload |

## Kafka Consumer Configuration

Custom `KafkaConsumerConfig` bean overrides Spring auto-config:

| Property | Value | Purpose |
|---|---|---|
| `key-deserializer` | `LongDeserializer` | Matches producer's Long key |
| `value-deserializer` | `JacksonJsonDeserializer` | Deserializes JSON to `OrderEvent` |
| `auto-offset-reset` | `earliest` | Start from beginning for new groups |
| `isolation.level` | `read_committed` | Only reads committed transactional messages |
| `fetch-min-bytes` | `1` | Long polling: respond immediately when data available |
| `fetch-max-wait-ms` | `5000` | Long polling: broker waits up to 5s if no data |
| `session-timeout-ms` | `30000` | Consumer considered dead after 30s of no heartbeat |
| `concurrency` | `3` | 3 consumer threads matching 3 partitions |

## Retry Topics & Dead Letter Topic

`@RetryableTopic(attempts=4, backoff=@BackOff(delay=1000, multiplier=2.0))` on the listener:
- If processing fails, message is sent to retry topics (`order-events-retry-0`, `-retry-1`, `-retry-2`)
- Exponential backoff: 1s → 2s → 4s
- After 4 total attempts, message goes to DLT (`order-events-dlt`)
- `@DltHandler` saves notification with FAILED status to DB
- `SUFFIX_WITH_INDEX_VALUE` strategy for clear retry topic naming

## Multi-Broker Cluster

Connects to 3 Kafka brokers (`localhost:9092,9093,9094`). `read_committed` isolation ensures the consumer only sees messages from committed producer transactions.

## Idempotency

The consumer checks `notificationRepository.existsByOrderId()` before processing — if a notification was already sent for an order, it skips reprocessing. This prevents duplicate emails on message redelivery.

## Email Configuration

Requires environment variables:

```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password    # Gmail App Password
```

Uses TLS 1.2 via Gmail SMTP on port 587.

## Database

Uses H2 in-memory (`jdbc:h2:mem:notification`). Tables auto-created via `ddl-auto: create-drop`.

H2 Console: [http://localhost:8086/notification-service/h2](http://localhost:8086/notification-service/h2)

## Actuator

Enabled endpoints: `health`, `metrics`, `info`

Health: [http://localhost:8086/notification-service/actuator/health](http://localhost:8086/notification-service/actuator/health)
