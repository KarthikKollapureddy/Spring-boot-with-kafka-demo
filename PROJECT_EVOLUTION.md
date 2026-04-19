# Project Evolution — From Initial State to Final Implementation

A chronological log of every improvement made to this Kafka project, what was wrong, what was fixed, and what was learned at each step.

---

## Initial State (Starting Point)

**Score: 7.9/10**

A working Spring Boot + Kafka project with two microservices:
- **orderService** (producer) — accepts orders via REST, saves to H2, publishes to Kafka
- **notificationService** (consumer) — consumes order events, sends email, saves notification status

### What was already good
- `KafkaTemplate` with async `whenComplete()` callback
- `acks=all` + `enable-idempotence=true` + `retries=3` (producer reliability)
- `@KafkaListener` with manual acknowledgment
- Long polling config (`fetch-min-bytes`, `fetch-max-wait-ms`)
- Consumer idempotency via `existsByOrderId()` check
- Docker Compose with Zookeeper + Kafka + Kafka UI + health checks
- Java 21 records for DTOs and events
- Interface-based service layer with constructor injection
- Email masking utility for log privacy

### What had bugs
- `@Transactional` on private methods
- Missing `@RequestBody` on controller
- `System.out.println` in production code
- Duplicate consumer config (YAML + Java)
- Unused `ISO_FORMATTER` constant
- `@Service` on interfaces instead of implementations
- No error handling strategy (`@ControllerAdvice`)
- 19 scattered markdown files with no organization

---

## Revision 1 — Cleanup & Bug Fixes

**Score: 7.9 → 8.1/10**

### Changes
| Fix | File | What was wrong |
|---|---|---|
| `private` → `public` on `saveOrderToDB()` and `saveProcessOrderToDB()` | `OrderProcessorImpl.java` | `@Transactional` on private methods does nothing — Spring uses proxies to intercept method calls, and proxies can't intercept private methods. The DB operations were running without transaction boundaries. |
| Added `@RequestBody` | `OrdersController.java` | Without `@RequestBody`, Spring doesn't know to deserialize the HTTP body JSON into the `OrderRequestDTO` object. The parameter would be null. |
| Removed `System.out.println` | `OrderProcessorImpl.java` | `System.out.println` bypasses the logging framework (SLF4J/Logback). It can't be filtered by log level, doesn't include timestamps/thread info, and can't be routed to log files. |

### Learning
> Spring's `@Transactional` works via AOP proxies. The proxy wraps the bean and intercepts method calls to manage transaction begin/commit/rollback. But it can only intercept calls that go through the proxy — internal `private` method calls bypass it entirely. Always make `@Transactional` methods `public`.

---

## Revision 2 — Multiple Partitions & Consumer Concurrency

**Score: 8.1 → 8.4/10**

### Changes
| Change | File | What was added |
|---|---|---|
| `@Bean` on `orderEventsTopic()` | `OrderEventTopic.java` | Method existed but wasn't a Spring bean — topic was never created programmatically |
| `partitions(3)` with Javadoc | `OrderEventTopic.java` | Explicit topic creation with 3 partitions instead of relying on auto-create (1 partition) |
| `replicas: 2` → `replicas: 1` | `application.yaml` | Replicas can't exceed broker count. We have 1 broker in docker-compose, so replicas must be ≤ 1 |
| `setConcurrency(3)` | `KafkaConsumerConfig.java` | Match consumer threads to partition count for maximum parallel consumption |

### Learning
> With 1 partition, only 1 consumer thread can read from a topic — no matter how many threads you configure. Kafka's parallelism unit is the partition. N partitions = up to N concurrent consumers in a consumer group. The producer's key (orderId) is hashed to determine the partition, so same orderId → same partition → guaranteed ordering per order, while different orders process in parallel.

---

## Revision 3 — Retry Topics & Dead Letter Topic (DLT)

**Score: 8.4 → 9.0/10**

### Changes
| Change | File | What was added |
|---|---|---|
| `@RetryableTopic(attempts=4, backoff=1s×2.0)` | `OrderEventConsumer.java` | Automatic retry with exponential backoff: 1s → 2s → 4s. Creates retry topics per attempt. |
| `@DltHandler` | `OrderEventConsumer.java` | Catches permanently failed messages after all retries exhausted. Saves FAILED status to DB. |
| `private` → `public` on listener | `OrderEventConsumer.java` | `@RetryableTopic` needs to proxy the method — private methods can't be proxied |
| Removed `Acknowledgment` param | `OrderEventConsumer.java` | `@RetryableTopic` manages acks internally using RECORD mode. Manual ack param conflicts. |
| Removed `AckMode.MANUAL` | `KafkaConsumerConfig.java` | `@RetryableTopic` requires RECORD ack mode. Manual mode blocks the retry-topic routing — Spring needs to ack the original message after copying it to the retry topic. |
| Removed FAILED save from catch block | `NotificationServiceImpl.java` | Critical fix: the old catch block saved a FAILED notification to DB on every failure. On retry, `existsByOrderId()` would find that FAILED row and return early — **the retry would never actually retry the email**. Now only `@DltHandler` writes FAILED (once, after all retries exhausted). |

### How @RetryableTopic works internally
```
Spring creates at startup:
  order-events              ← your listener reads from here
  order-events-retry-0      ← hidden listener (1s delay)
  order-events-retry-1      ← hidden listener (2s delay)
  order-events-retry-2      ← hidden listener (4s delay)
  order-events-dlt          ← @DltHandler reads from here

On failure:
  1. Exception thrown in your listener
  2. Spring catches it (RetryTopicExceptionRouter)
  3. Publishes SAME message to next retry topic with backoff-timestamp header
  4. ACKs the original (safe — copy exists in retry topic)
  5. Retry topic listener checks timestamp header → pauses consumer until delay passes
  6. Processes again → if still fails → next retry topic
  7. All retries exhausted → message goes to DLT → @DltHandler runs
```

### Learning
> The old approach (don't-ack on failure → Kafka redelivers) has a critical flaw: it **blocks the entire partition**. No other messages on that partition get processed until the failed one succeeds. If the email server is down for 2 hours, that partition is stuck. Retry topics solve this — the failed message moves to a separate topic, the original partition keeps flowing, and retries happen with exponential backoff. The DLT provides a final safety net for permanently failing messages.

> When combining `@RetryableTopic` with idempotency checks (`existsByOrderId()`), be careful about **what** triggers the idempotency. Saving FAILED rows in the catch block makes the retry skip itself — the idempotency check should only match SUCCESS records, or only the DLT handler should save FAILED.

---

## Revision 4 — Kafka Transactions (Exactly-Once End-to-End)

**Score: 9.0 → 9.2/10**

### Changes
| Change | File | What was added |
|---|---|---|
| `transaction-id-prefix: order-tx-` | `application.yaml` | Enables Kafka transactions on the producer. Each instance gets a unique transactional ID. |
| `executeInTransaction()` | `OrderProducer.java` | Wraps the send in a Kafka transaction: begin → send (invisible to consumers) → commit/abort |
| `isolation.level=read_committed` | `KafkaConsumerConfig.java` | Consumer only reads committed messages. Without this, consumer could read messages from aborted transactions — defeating the purpose. |

### The problem that was solved
```
BEFORE (no transactions):
  saveOrderToDB()       ✅ succeeds, order is in DB
  saveProcessOrderToDB() ✅ succeeds
  kafkaTemplate.send()  ❌ FAILS (network error, broker down, etc.)
  
  Result: Order exists in DB, but NO event was ever published.
  Customer never gets email. System is in inconsistent state.
  
AFTER (with transactions):
  executeInTransaction {
    send()              → message is buffered but INVISIBLE to consumers
  }
  → commit             → message becomes visible atomically
  → OR abort           → message is discarded, as if it was never sent
```

### How Kafka transactions work internally
```
Producer                          Kafka Broker
   │                                   │
   ├── initTransactions() ────────────▶│  registers transactional.id
   ├── beginTransaction() ────────────▶│  starts TX
   ├── send(record) ─────────────────▶│  written to log but INVISIBLE
   ├── commitTransaction() ───────────▶│  control message written, records visible
   │                                   │
   Consumer (read_committed) ─────────▶│  only reads after commit marker
   Consumer (read_uncommitted) ───────▶│  reads immediately (dangerous)
```

### Learning
> Kafka transactions provide exactly-once semantics at the **end-to-end level**. The key insight: `acks=all` + `enable-idempotence=true` only prevents duplicate **sends** — it doesn't help when the send itself fails after the DB write succeeds. Transactions make the send atomic: either the message is committed and visible to consumers, or it's aborted and invisible. The consumer **must** use `isolation.level=read_committed` — without it, the consumer runs in `read_uncommitted` mode (the default) and can read messages that later get aborted.

> Note: `transaction-id-prefix` automatically enforces `acks=all` and `enable-idempotence=true`. You can keep them explicit in YAML for clarity, but they're redundant when transactional.

---

## State after Revision 4

**Score: 9.2/10**

### Concepts Covered

| Concept | Implementation |
|---|---|
| Kafka Producer with KafkaTemplate | `OrderProducer` with async `whenComplete()` callback |
| Key-based partitioning | `orderId` as Long key → consistent partition assignment |
| JSON serialization/deserialization | `JsonSerializer` / `JacksonJsonDeserializer` |
| Acks + Idempotent Producer | `acks=all` + `enable-idempotence=true` |
| Producer retries with batching | `retries=3`, `linger.ms=10`, `batch.size=16384` |
| Consumer groups | `notification-service-group` |
| Long polling | `fetch-min-bytes=1`, `fetch-max-wait-ms=5000` |
| Multiple partitions | 3 partitions via `TopicBuilder` |
| Consumer concurrency | `setConcurrency(3)` matching partition count |
| Retry topics | `@RetryableTopic` with exponential backoff (1s × 2.0) |
| Dead Letter Topic | `@DltHandler` saves FAILED status after all retries exhausted |
| Consumer idempotency | `existsByOrderId()` prevents duplicate processing |
| Kafka Transactions | `executeInTransaction()` + `read_committed` isolation |
| Manual → Auto ack migration | Removed `AckMode.MANUAL` for `@RetryableTopic` compatibility |
| Custom consumer config | Programmatic `ConsumerFactory` with `KafkaConsumerConfig` |
| Docker Compose infrastructure | Zookeeper + Kafka + Kafka UI with health checks |
| Email notifications | `JavaMailSender` with Gmail SMTP + TLS 1.2 |
| Spring Actuator | Health, metrics, info endpoints |
| OpenAPI/Swagger | API documentation on both services |

### Still open for exploration

| Concept | Description |
|---|---|
| Schema Registry | Avro/Protobuf with centralized schema management instead of duplicating `OrderEvent` records |
| Integration Tests | `@EmbeddedKafka` for end-to-end Kafka tests |
| Circuit Breaker | Resilience4j dependency exists but isn't wired up |
| Monitoring | Kafka metrics via Micrometer/Prometheus |

---

## Revision 5 — Multi-Broker Cluster (High Availability)

**Score: 9.2 → 9.5/10**

### Changes
| Change | File | What was added |
|---|---|---|
| 3-broker cluster | `docker-compose.yml` | `kafka-0` (9092), `kafka-1` (9093), `kafka-2` (9094) — each with health checks and `kafka-network` |
| `KAFKA_DEFAULT_REPLICATION_FACTOR: 3` | `docker-compose.yml` | New topics auto-replicate to all 3 brokers |
| `KAFKA_MIN_INSYNC_REPLICAS: 2` | `docker-compose.yml` | At least 2 replicas must ack before write is considered successful |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3` | `docker-compose.yml` | Consumer offsets (which messages have been read) are also replicated |
| `bootstrap-servers` updated | Both `application.yaml` | `localhost:9092,localhost:9093,localhost:9094` — all 3 brokers listed |
| `replicas: 3` | orderService `application.yaml` | Topic `order-events` replicated to all 3 brokers |
| `kafka-ui` updated | `docker-compose.yml` | Depends on all 3 brokers, connects to all 3 bootstrap servers |
| Fixed nesting bug | `docker-compose.yml` | `kafka-1` and `kafka-2` were indented inside `kafka-0` — moved to correct level |

### The problem that was solved
```
BEFORE (single broker):
  ┌──────────────┐
  │ Kafka Broker │  ← if this dies, everything is gone
  │ partition-0  │     no data, no producers, no consumers
  │ partition-1  │
  │ partition-2  │
  └──────────────┘

AFTER (3-broker cluster with replication-factor=3):
  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
  │  Broker 0    │  │  Broker 1    │  │  Broker 2    │
  │ P0 (leader)  │  │ P0 (replica) │  │ P0 (replica) │
  │ P1 (replica) │  │ P1 (leader)  │  │ P1 (replica) │
  │ P2 (replica) │  │ P2 (replica) │  │ P2 (leader)  │
  └──────────────┘  └──────────────┘  └──────────────┘

  Broker 1 dies → Broker 0/2 elect new leaders → zero data loss, zero downtime
```

### How min.insync.replicas + acks=all work together
```
Scenario: acks=all + min.insync.replicas=2 + replication.factor=3

  Producer sends message → Broker (leader) receives it
    → Leader writes to its log
    → Waits for at least 1 more replica (min.insync.replicas=2 total) to confirm
    → Both have the data → returns success to producer

  If ISR drops to 1 (only leader left, both followers behind):
    → Producer gets NotEnoughReplicasException
    → REFUSES to write rather than risk data loss
    → This is the strongest durability guarantee in Kafka
```

### Learning
> Multi-broker is purely an **infrastructure change** — application code doesn't change at all. Your `acks=all`, `enable-idempotence`, `retries`, and `transactions` already work correctly with multiple brokers. The replication factor and min.insync.replicas are broker-level settings that provide fault tolerance underneath. The key insight: `acks=all` means "wait for all **in-sync** replicas" — with 1 broker that's just 1 ack (useless), with 3 brokers and `min.insync.replicas=2` it means the data exists on at least 2 machines before the producer gets success.

---

## Final State

**Score: 9.5/10**

### Concepts Covered

| Concept | Implementation |
|---|---|
| Kafka Producer with KafkaTemplate | `OrderProducer` with async `whenComplete()` callback |
| Key-based partitioning | `orderId` as Long key → consistent partition assignment |
| JSON serialization/deserialization | `JsonSerializer` / `JacksonJsonDeserializer` |
| Acks + Idempotent Producer | `acks=all` + `enable-idempotence=true` |
| Producer retries with batching | `retries=3`, `linger.ms=10`, `batch.size=16384` |
| Consumer groups | `notification-service-group` |
| Long polling | `fetch-min-bytes=1`, `fetch-max-wait-ms=5000` |
| Multiple partitions | 3 partitions via `TopicBuilder` |
| Consumer concurrency | `setConcurrency(3)` matching partition count |
| Retry topics | `@RetryableTopic` with exponential backoff (1s × 2.0) |
| Dead Letter Topic | `@DltHandler` saves FAILED status after all retries exhausted |
| Consumer idempotency | `existsByOrderId()` prevents duplicate processing |
| Kafka Transactions | `executeInTransaction()` + `read_committed` isolation |
| Manual → Auto ack migration | Removed `AckMode.MANUAL` for `@RetryableTopic` compatibility |
| Custom consumer config | Programmatic `ConsumerFactory` with `KafkaConsumerConfig` |
| **Multi-broker cluster** | **3 Kafka brokers with `replication-factor=3`, `min.insync.replicas=2`** |
| Docker Compose infrastructure | Zookeeper + 3 Kafka brokers + Kafka UI with health checks |
| Email notifications | `JavaMailSender` with Gmail SMTP + TLS 1.2 |
| Spring Actuator | Health, metrics, info endpoints |
| OpenAPI/Swagger | API documentation on both services |

### Still open for exploration

| Concept | Description |
|---|---|
| Schema Registry | Avro/Protobuf with centralized schema management instead of duplicating `OrderEvent` records |
| Integration Tests | `@EmbeddedKafka` for end-to-end Kafka tests |
| Circuit Breaker | Resilience4j dependency exists but isn't wired up |
| Monitoring | Kafka metrics via Micrometer/Prometheus |
