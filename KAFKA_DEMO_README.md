# Kafka Producer-Consumer Demo: Order Processing & Notification System

A **comprehensive enterprise-grade microservices demo** showcasing Apache Kafka for distributed event-driven architecture with Spring Boot. This project demonstrates a complete producer-consumer pattern with order processing and email notification workflows.

---

## 📚 Table of Contents

1. [System Architecture](#-system-architecture)
2. [Project Structure](#-project-structure)
3. [Components Overview](#-components-overview)
4. [Quick Start](#-quick-start)
5. [Producer Service (Order Service)](#-producer-service-order-service)
6. [Consumer Service (Notification Service)](#-consumer-service-notification-service)
7. [Kafka Configuration](#-kafka-configuration)
8. [Testing & Demo](#-testing--demo)
9. [Production Considerations](#-production-considerations)
10. [Troubleshooting](#-troubleshooting)

---

## 🏗️ System Architecture

### High-Level Message Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CLIENT (HTTP REST)                              │
│              POST /app/publish (JSON Order Request)                 │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   ORDER SERVICE (PRODUCER)                          │
│                      Port: 8080                                     │
│  • Validates order request                                          │
│  • Saves to H2 database (Orders table)                             │
│  • Publishes OrderEvent to Kafka Topic                            │
│  • Returns OrderResponseDTO to client                             │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
╔═════════════════════════════════════════════════════════════════════╗
║                    KAFKA BROKER                                     ║
║              Topic: order-events                                    ║
║  ┌─────────────────────────────────────────────────────────────┐   ║
║  │ Partition 0: [OrderEvent1, OrderEvent3, OrderEvent5, ...]  │   ║
║  │ Partition 1: [OrderEvent2, OrderEvent4, OrderEvent6, ...]  │   ║
║  │ Partition 2: [OrderEvent7, OrderEvent8, OrderEvent9, ...]  │   ║
║  └─────────────────────────────────────────────────────────────┘   ║
║  Replication Factor: 3                                              ║
║  Partitioning Strategy: By orderId (Long key) - ensures order      ║
║  Processing Guarantee: At-least-once delivery semantics            ║
╚═════════════════════════════════════════════════════════════════════╝
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│            NOTIFICATION SERVICE (CONSUMER) [TODO]                   │
│                      Port: 8081                                     │
│  • Consumes OrderEvent from Kafka Topic                            │
│  • Processes events with manual acknowledgment                     │
│  • Sends SMTP email notification (To be implemented)              │
│  • Stores notification status in database                         │
│  • Handles dead-letter queue for failed notifications             │
└─────────────────────────────────────────────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
    │  SMTP Server│  │  H2 Database │  │ DLQ Topic    │
    │ (Gmail/etc) │  │  (Logs/Audit)│  │  (Retries)   │
    └─────────────┘  └──────────────┘  └──────────────┘
```

---

## 📁 Project Structure

```
Spring-boot-with-kafka/
│
├── orderService/                              # PRODUCER (Ready)
│   ├── src/main/java/com/kafkaProducer/orderService/
│   │   ├── OrderServiceApplication.java       # Spring Boot main
│   │   ├── config/
│   │   │   └── JacksonConfig.java             # DateTime deserialization
│   │   ├── controller/
│   │   │   └── OrdersController.java          # REST endpoints
│   │   ├── service/
│   │   │   ├── OrderProcessor.java            # Service interface
│   │   │   ├── OrderProcessorImpl.java         # Business logic
│   │   │   └── OrderProducer.java             # Kafka publisher
│   │   ├── entity/
│   │   │   ├── Orders.java                    # Order entity
│   │   │   └── ProcessedOrders.java           # Idempotency tracking
│   │   ├── event/
│   │   │   └── OrderEvent.java                # Message event (record)
│   │   ├── dto/
│   │   │   ├── OrderRequestDTO.java           # Request DTO
│   │   │   └── OrderResponseDTO.java          # Response DTO
│   │   └── repository/
│   │       ├── OrdersRepository.java          # JPA repo
│   │       └── ProcessedOrderRepository.java  # Idempotency repo
│   ├── src/main/resources/
│   │   └── application.yaml                   # Producer config
│   └── pom.xml
│
├── notificationService/                       # CONSUMER (TODO)
│   ├── src/main/java/com/kafkaConsumer/notificationService/
│   │   ├── NotificationServiceApplication.java
│   │   ├── config/
│   │   │   └── KafkaConsumerConfig.java
│   │   ├── controller/
│   │   │   └── NotificationController.java    # Health/status endpoints
│   │   ├── service/
│   │   │   ├── OrderEventConsumer.java        # Kafka listener
│   │   │   └── NotificationService.java       # Email sender
│   │   ├── entity/
│   │   │   └── Notification.java              # Notification record
│   │   ├── event/
│   │   │   └── OrderEvent.java                # Same event from producer
│   │   ├── dto/
│   │   │   └── NotificationStatusDTO.java
│   │   ├── repository/
│   │   │   └── NotificationRepository.java
│   │   └── exception/
│   │       └── NotificationException.java
│   ├── src/main/resources/
│   │   └── application.yaml                   # Consumer config
│   └── pom.xml
│
├── docker-compose.yml                         # Kafka & Zookeeper setup
├── KAFKA_DEMO_README.md                       # This file
└── .gitignore
```

---

## 🔧 Components Overview

### 1. **Producer: Order Service** ✅ READY

**Purpose**: Accept HTTP requests to create orders and publish them to Kafka

| Component | Purpose |
|-----------|---------|
| **OrdersController** | REST endpoint: `POST /app/publish` |
| **OrderProcessor** | Business logic orchestrator |
| **OrderProducer** | Kafka publisher using KafkaTemplate |
| **OrderEvent** | Message payload (Java record) |
| **Orders Entity** | Persists order data |
| **ProcessedOrders Entity** | Tracks processed orders for idempotency |

**Key Features**:
- ✅ Exactly-once semantics via idempotent producer
- ✅ Manual Kafka retries (up to 3 times)
- ✅ ISO-8601 datetime support
- ✅ Async callback for publisher success/failure
- ✅ H2 in-memory database (dev) / Can use any SQL DB

**Configuration** (`application.yaml`):
```yaml
kafka:
  bootstrap-servers: localhost:9092
  topic:
    name: order-events
  producer:
    key-serializer: LongSerializer
    value-serializer: JsonSerializer
    acks: all
    enable-idempotence: true
    retries: 3
```

---

### 2. **Consumer: Notification Service** 📋 TODO

**Purpose**: Consume order events from Kafka and send email notifications

#### Planned Implementation Details:

| Component | Purpose |
|-----------|---------|
| **OrderEventConsumer** | `@KafkaListener` method to process events |
| **NotificationService** | Business logic for email sending |
| **SmtpConfig** | SMTP configuration for email provider |
| **Notification Entity** | Audit trail of sent notifications |
| **NotificationController** | Health checks & status endpoints |

#### Key Features to Implement:

1. **Manual Message Acknowledgment**
   - Commit offset only after successful email send
   - Handle failed messages with dead-letter queue (DLQ)

2. **Error Handling & Retry Logic**
   - Exponential backoff for transient failures
   - DLQ for permanent failures
   - Comprehensive logging

3. **Email Notification**
   - SMTP integration (Gmail, AWS SES, etc.)
   - Template-based email composition
   - Async email sending

4. **Database Persistence**
   - Log notification attempts
   - Track success/failure status
   - Audit trail of all notifications

5. **Idempotency**
   - Prevent duplicate notifications (track by orderId)
   - Deduplicate if message is redelivered

#### Configuration Template (TODO):

```yaml
kafka:
  bootstrap-servers: localhost:9092
  topic:
    name: order-events
  consumer:
    group-id: notification-service-group
    auto-offset-reset: earliest  # Start from beginning if group is new
    key-deserializer: LongDeserializer
    value-deserializer: JsonDeserializer
    # Manual commit - only commit after processing
    enable-auto-commit: false
    # Wait max 1 second for batch of messages
    fetch-min-bytes: 1
    fetch-max-wait-ms: 1000

# Email configuration
mail:
  host: smtp.gmail.com
  port: 587
  username: ${GMAIL_USERNAME}
  password: ${GMAIL_APP_PASSWORD}
  from: orders@company.com
  properties:
    transport:
      protocol: smtp
    smtp:
      auth: true
      starttls:
        enable: true
        required: true
      ssl:
        protocols: TLSv1.2
```

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (for Kafka)
- An SMTP account (Gmail, AWS SES, Mailgun, etc.) - for email notifications

### Step 1: Start Kafka & Zookeeper

```bash
cd /Users/karthikkollapureddy/Documents/Spring-boot-with-kafka

# Create docker-compose.yml if not exists
docker-compose up -d

# Verify Kafka is running
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Step 2: Build & Run Order Service (Producer)

```bash
cd orderService
mvn clean install
mvn spring-boot:run
```

**Service starts on**: `http://localhost:8080`

Verify it's running:
```bash
curl http://localhost:8080/swagger-ui.html
```

### Step 3: Create an Order (Test Producer)

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop", "Mouse"],
    "amount": 999.99,
    "paymentDateTime": "2024-04-19T10:30:00"
  }'
```

**Expected Response**:
```json
{
  "id": 1,
  "products": ["Laptop", "Mouse"],
  "amount": 999.99,
  "paymentDateTime": "2024-04-19T10:30:00",
  "message": "Order published successfully"
}
```

### Step 4: Verify Message in Kafka

```bash
# List topics
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Read messages from topic (from beginning)
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --property print.key=true
```

You should see the OrderEvent message in JSON format.

---

## 📤 Producer Service (Order Service)

### REST API

#### **Create Order**
```http
POST /app/publish
Content-Type: application/json

{
  "products": ["Laptop", "Mouse Pad"],
  "amount": 1200.50,
  "paymentDateTime": "2024-04-19T10:30:00"
}
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "products": ["Laptop", "Mouse Pad"],
  "amount": 1200.50,
  "paymentDateTime": "2024-04-19T10:30:00",
  "message": "Order published successfully"
}
```

### Message Flow

1. **OrdersController** validates the request
2. **OrderProcessorImpl**:
   - Parses ISO-8601 datetime
   - Saves Order to H2 database
   - Checks ProcessedOrders table (idempotency)
   - Creates OrderEvent
3. **OrderProducer** publishes event to Kafka:
   - Key: `orderId` (Long)
   - Value: `OrderEvent` (JSON serialized)
   - Topic: `order-events`
4. Kafka acknowledges with `acks=all` (all replicas confirmed)
5. Returns OrderResponseDTO with success message

### OrderEvent Message Structure

```java
public record OrderEvent(
    Long id,                      // Unique order identifier
    List<String> products,        // List of products ordered
    Float amount,                 // Total order amount
    LocalDateTime paymentDateTime // Payment timestamp
) {}
```

**Serialized as JSON in Kafka**:
```json
{
  "id": 1,
  "products": ["Item1", "Item2"],
  "amount": 999.99,
  "paymentDateTime": "2024-04-19T10:30:00"
}
```

### Producer Guarantees

| Guarantee | Mechanism | Benefit |
|-----------|-----------|---------|
| **At-least-once delivery** | Retries (3x) + acks=all | Messages won't be lost |
| **Exactly-once semantics** | Idempotent producer flag | No duplicates at producer side |
| **Ordered delivery** | Partitioning by orderId | Messages from same order stay ordered |
| **Durability** | Replication factor 3 | Survives broker failures |

---

## 📥 Consumer Service (Notification Service)

### Architecture

```
Kafka Topic (order-events)
    ↓
[OrderEventConsumer] - @KafkaListener
    ↓
[Message Processing]
    ├→ Extract order details
    ├→ Check idempotency (already processed?)
    └→ Send email via SMTP
    ↓
[Commit or DLQ]
    ├→ Success: Commit offset
    └→ Failure: Send to Dead-Letter Queue
```

### Implementation Steps (TODO)

#### **Step 1: Create NotificationService Project**

```bash
# Create new Spring Boot project for notification service
mvn archetype:generate \
  -DgroupId=com.kafkaConsumer \
  -DartifactId=notificationService \
  -DarchetypeArtifactId=maven-archetype-quickstart
```

#### **Step 2: Add Dependencies** (pom.xml)

```xml
<!-- Kafka Consumer -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>

<!-- Email Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Database (H2 for dev) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### **Step 3: Create Consumer Listener**

**OrderEventConsumer.java**:
```java
@Component
@Slf4j
public class OrderEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    @KafkaListener(
        topics = "order-events",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
        OrderEvent event,
        Acknowledgment ack,
        ConsumerRecord<Long, OrderEvent> record
    ) {
        try {
            log.info("Received order event: {}", event.id());
            
            // Process notification
            notificationService.sendOrderNotification(event);
            
            // Commit offset only after successful processing
            ack.acknowledge();
            log.info("Order {} notification sent successfully", event.id());
            
        } catch (Exception e) {
            log.error("Failed to process order {}: {}", event.id(), e.getMessage());
            // Don't acknowledge - message will be reprocessed
            // Could also send to DLQ here
            throw new RuntimeException("Processing failed", e);
        }
    }
}
```

#### **Step 4: Implement Email Sending**

**NotificationService.java**:
```java
@Service
@Slf4j
public class NotificationService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    public void sendOrderNotification(OrderEvent event) throws Exception {
        // Check idempotency
        if (notificationRepository.existsByOrderId(event.id())) {
            log.warn("Notification already sent for order {}", event.id());
            return;
        }
        
        try {
            // Create email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.customerEmail());
            message.setSubject("Order Confirmation #" + event.id());
            message.setText(buildEmailBody(event));
            
            // Send email
            mailSender.send(message);
            
            // Log success
            Notification notification = new Notification();
            notification.setOrderId(event.id());
            notification.setStatus("SUCCESS");
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            log.info("Email sent for order {}", event.id());
            
        } catch (MailException e) {
            log.error("Failed to send email for order {}: {}", event.id(), e);
            throw new RuntimeException("Email send failed", e);
        }
    }
    
    private String buildEmailBody(OrderEvent event) {
        return String.format(
            "Dear Customer,\n\n" +
            "Your order #%d has been processed.\n" +
            "Products: %s\n" +
            "Amount: $%.2f\n" +
            "Payment Date: %s\n\n" +
            "Thank you for your purchase!",
            event.id(),
            String.join(", ", event.products()),
            event.amount(),
            event.paymentDateTime()
        );
    }
}
```

#### **Step 5: Configure Kafka Consumer**

**application.yaml**:
```yaml
spring:
  application:
    name: notificationService
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
  datasource:
    username: sa
    password: sa
    url: jdbc:h2:mem:notification
  h2:
    console:
      enabled: true
  
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest  # Start from beginning if group new
      key-deserializer: org.apache.kafka.common.serialization.LongDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.type.mapping: orderEvent:com.kafkaConsumer.notificationService.event.OrderEvent
      enable-auto-commit: false  # Manual acknowledgment
      
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            protocols: TLSv1.2

server:
  port: 8081
```

#### **Step 6: Create Notification Entity**

**Notification.java**:
```java
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING
    
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime sentAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

## ⚙️ Kafka Configuration

### Producer Configuration Details

| Property | Value | Explanation |
|----------|-------|-------------|
| `bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `topic.name` | `order-events` | Topic to publish to |
| `key-serializer` | `LongSerializer` | Serializes order ID |
| `value-serializer` | `JsonSerializer` | Serializes OrderEvent as JSON |
| `acks` | `all` | Wait for all in-sync replicas |
| `enable-idempotence` | `true` | Prevents duplicate messages |
| `retries` | `3` | Retry failed sends 3 times |
| `linger.ms` | `10` | Batch wait time in ms |
| `batch.size` | `16384` | Batch size in bytes |

### Consumer Configuration Details

| Property | Value | Explanation |
|----------|-------|-------------|
| `bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `group-id` | `notification-service-group` | Consumer group identifier |
| `auto-offset-reset` | `earliest` | Start from beginning if new |
| `key-deserializer` | `LongDeserializer` | Deserializes order ID |
| `value-deserializer` | `JsonDeserializer` | Deserializes to OrderEvent |
| `enable-auto-commit` | `false` | Manual offset management |
| `fetch-min-bytes` | `1` | Fetch at least 1 byte |
| `fetch-max-wait-ms` | `1000` | Max wait time for batch |

### Topic Configuration

```bash
# Create topic with 3 partitions and replication factor 3
docker exec -it kafka kafka-topics.sh \
  --create \
  --topic order-events \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092 \
  --config retention.ms=604800000

# Verify topic creation
docker exec -it kafka kafka-topics.sh \
  --describe \
  --topic order-events \
  --bootstrap-server localhost:9092

# Create Dead-Letter Queue topic for failed messages
docker exec -it kafka kafka-topics.sh \
  --create \
  --topic order-events-dlq \
  --partitions 1 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

---

## 🧪 Testing & Demo

### Test Producer

#### Test 1: Single Order
```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop"],
    "amount": 999.99,
    "paymentDateTime": "2024-04-19T10:30:00"
  }' | jq .
```

#### Test 2: Multiple Orders (Bulk Test)
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/app/publish \
    -H "Content-Type: application/json" \
    -d "{
      \"products\": [\"Product$i\"],
      \"amount\": $((100 + i * 50)).99,
      \"paymentDateTime\": \"2024-04-19T1$i:00:00\"
    }"
  echo "Order $i created"
  sleep 0.5
done
```

#### Test 3: Read Messages from Kafka
```bash
# From beginning
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092

# From latest
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --bootstrap-server localhost:9092

# With key and value
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --property print.key=true \
  --property print.value=true
```

### Monitor Kafka Cluster

```bash
# Check consumer group status
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list

# Describe consumer group
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-service-group

# Check topic details
docker exec -it kafka kafka-topics.sh \
  --describe \
  --topic order-events \
  --bootstrap-server localhost:9092

# View topic metrics
docker exec -it kafka kafka-log-segments.sh \
  --describe \
  --topic-log /var/lib/kafka/data/order-events-0
```

### Verify Idempotency

```bash
# Send same order twice with same ID
# The second request should be idempotent (no duplicate in DB)
# But Kafka might have both (depends on retry logic)

curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Test Product"],
    "amount": 50.00,
    "paymentDateTime": "2024-04-19T11:00:00"
  }'

# Send again immediately
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Test Product"],
    "amount": 50.00,
    "paymentDateTime": "2024-04-19T11:00:00"
  }'

# Check database - should have only 1 order (ProcessedOrders table)
# Check Kafka - might have both messages (producer-level dedup works but retries still send)
```

---

## 🏭 Production Considerations

### 1. **Error Handling & Resilience**

- ✅ Circuit breaker for SMTP failures
- ✅ Exponential backoff for retries
- ✅ Dead-Letter Queue for permanent failures
- ✅ Comprehensive logging with correlation IDs

### 2. **Monitoring & Observability**

```yaml
# Add Spring Boot Actuator
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

# Metrics endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Key Metrics to Monitor**:
- `kafka.consumer.records.lag` - How far behind are we
- `kafka.consumer.fetch.total.records` - Records processed
- `kafka.producer.record.send.total` - Records sent
- `mail.send.time` - Email send latency
- Custom: `notification.success.count`, `notification.failure.count`

### 3. **Configuration Management**

```yaml
# Use environment variables for sensitive data
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

mail:
  username: ${MAIL_USERNAME}
  password: ${MAIL_PASSWORD}  # Use app password, not account password
```

### 4. **Security**

- ✅ SASL/SCRAM authentication for Kafka
- ✅ SSL/TLS encryption for Kafka & SMTP
- ✅ API authentication (OAuth2, JWT)
- ✅ Database encryption at rest
- ✅ Secrets management (Vault, AWS Secrets Manager)

### 5. **Scalability**

- Partition strategy: By `orderId` ensures ordered processing per customer
- Multiple consumer instances in same group for parallel processing
- Message batching to reduce network overhead
- Connection pooling for SMTP and database

### 6. **Deployment**

```yaml
# Docker deployment example
version: '3.8'
services:
  order-service:
    build: ./orderService
    ports:
      - "8080:8080"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  
  notification-service:
    build: ./notificationService
    ports:
      - "8081:8081"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
  
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
```

---

## 🔧 Troubleshooting

### Issue: "Connection refused" to Kafka

```bash
# Verify Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Verify broker is listening
docker exec -it kafka netstat -tulpn | grep 9092

# Test connection
docker exec -it kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Issue: Messages not appearing in Kafka

```bash
# Check producer logs for errors
docker logs order-service

# Verify topic exists
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check topic configuration
docker exec -it kafka kafka-topics.sh --describe --topic order-events --bootstrap-server localhost:9092
```

### Issue: Consumer not processing messages

```bash
# Check consumer group status
docker exec -it kafka kafka-consumer-groups.sh \
  --describe \
  --group notification-service-group \
  --bootstrap-server localhost:9092

# Check for errors in consumer logs
docker logs notification-service

# Verify consumer can reach Kafka
docker exec -it notification-service curl -v kafka:9092
```

### Issue: Emails not sending

```bash
# Check SMTP configuration
# - Correct host and port
# - Username and password correct
# - App password (not account password) for Gmail
# - 2-factor authentication enabled in email provider

# Test SMTP connection (from within container)
docker exec -it notification-service bash -c \
  "curl -v telnet://smtp.gmail.com:587"

# Check email logs
docker logs notification-service | grep -i "mail\|email\|smtp"
```

### Issue: Duplicate notifications sent

```bash
# Check Notification table for duplicate orderId entries
SELECT * FROM notifications WHERE order_id = ?;

# Verify idempotency logic in NotificationService.sendOrderNotification()

# Check if consumer group is configured for manual commit
# (enable-auto-commit must be false)
```

### Issue: High lag in consumer group

```bash
# Check lag
docker exec -it kafka kafka-consumer-groups.sh \
  --describe \
  --group notification-service-group \
  --bootstrap-server localhost:9092

# Increase consumer instances (parallel processing)
# Tune fetch size: fetch-min-bytes, fetch-max-wait-ms
# Check for slow SMTP operations (email sending)
```

---

## 📚 References & Further Reading

### Official Documentation
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Boot Kafka Integration](https://spring.io/projects/spring-kafka)
- [Kafka Best Practices](https://www.confluent.io/blog/kafka-best-practices/)

### Key Concepts
- **Exactly-Once Semantics**: Transactional writes + manual commits
- **Partitioning**: Ensures order and enables parallelism
- **Consumer Groups**: Distributed, fault-tolerant message processing
- **Dead-Letter Queues**: Handle permanently failed messages
- **Idempotency**: Prevent duplicates even with retries

### Related Technologies
- Spring Mail: Email configuration and sending
- Spring Data JPA: Database persistence
- Spring Boot Actuator: Monitoring and metrics
- Docker Compose: Local development setup

---

## 📝 Summary

This repository demonstrates a **complete Kafka producer-consumer pattern** with:

✅ **Producer (Order Service)**: Ready to accept orders and publish to Kafka  
📋 **Consumer (Notification Service)**: TODO - Implement email notifications  
⚙️ **Infrastructure**: Docker Compose for Kafka + Zookeeper  
📚 **Documentation**: Comprehensive guides and examples  

**Next Steps**:
1. Implement the Notification Service consumer
2. Add email notification functionality
3. Deploy to production infrastructure
4. Monitor with metrics and logging
5. Scale with multiple consumer instances

---

**Last Updated**: April 19, 2026  
**Maintainer**: Your Team  
**License**: MIT
