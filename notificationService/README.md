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
| `OrderEventConsumer` | `@KafkaListener` — consumes from `order-events` topic with manual acknowledgment |
| `NotificationService` / `NotificationServiceImpl` | Sends email via `JavaMailSender`, saves notification to DB |
| `KafkaConsumerConfig` | Custom consumer factory with `JacksonJsonDeserializer`, manual ack mode, error handler |
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
| `enable-auto-commit` | `false` | Manual acknowledgment only |
| `max-poll-records` | `10` | Bounded batch per poll |
| `fetch-min-bytes` | `1` | Long polling: respond immediately when data available |
| `fetch-max-wait-ms` | `5000` | Long polling: broker waits up to 5s if no data |
| `session-timeout-ms` | `30000` | Consumer considered dead after 30s of no heartbeat |
| `AckMode` | `MANUAL` | Must call `ack.acknowledge()` explicitly |

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
