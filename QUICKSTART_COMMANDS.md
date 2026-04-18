# Quick Start & Command Reference

Fast-track guide to get the Kafka producer-consumer demo running locally.

---

## 🚀 5-Minute Setup

### Step 1: Start Kafka & Zookeeper (1 minute)

```bash
cd /Users/karthikkollapureddy/Documents/Spring-boot-with-kafka

# Start Kafka cluster in background
docker-compose up -d

# Verify services are running
docker-compose ps
```

Expected output:
```
NAME            STATUS
zookeeper       running (healthy)
kafka           running (healthy)
kafka-ui        running (healthy)
```

### Step 2: Start Order Service (Producer) (2 minutes)

```bash
cd orderService
mvn clean install
mvn spring-boot:run
```

Watch for message:
```
OrderServiceApplication started in X seconds
```

### Step 3: Create an Order (30 seconds)

In new terminal:
```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop"],
    "amount": 999.99,
    "paymentDateTime": "2024-04-19T10:30:00"
  }'
```

Expected response:
```json
{
  "id": 1,
  "products": ["Laptop"],
  "amount": 999.99,
  "message": "Order published successfully"
}
```

### Step 4: Verify Message in Kafka (30 seconds)

```bash
# View messages in topic
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092
```

Expected output:
```
{"id":1,"products":["Laptop"],"amount":999.99,"paymentDateTime":"2024-04-19T10:30:00"}
```

**✅ Producer is working!**

---

## 📚 Common Commands

### Kafka Management Commands

```bash
# List all topics
docker exec -it kafka kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092

# Describe a topic
docker exec -it kafka kafka-topics.sh \
  --describe \
  --topic order-events \
  --bootstrap-server localhost:9092

# Create a new topic
docker exec -it kafka kafka-topics.sh \
  --create \
  --topic my-topic \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092

# Delete a topic
docker exec -it kafka kafka-topics.sh \
  --delete \
  --topic my-topic \
  --bootstrap-server localhost:9092

# List consumer groups
docker exec -it kafka kafka-consumer-groups.sh \
  --list \
  --bootstrap-server localhost:9092

# Describe consumer group
docker exec -it kafka kafka-consumer-groups.sh \
  --describe \
  --group notification-service-group \
  --bootstrap-server localhost:9092

# Read messages from topic
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --property print.key=true

# Produce a test message
docker exec -it kafka kafka-console-producer.sh \
  --topic order-events \
  --broker-list localhost:9092 \
  --property "parse.key=true" \
  --property "key.separator=:"

# Reset consumer group offset
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --reset-offsets \
  --to-earliest \
  --execute
```

### Docker Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f kafka

# View specific container logs
docker logs -f kafka

# Execute command in container
docker exec -it kafka bash

# Remove all containers and volumes
docker-compose down -v

# Rebuild and restart
docker-compose down && docker-compose up -d

# Check container health
docker-compose ps
```

### Maven Commands

```bash
# Build project
mvn clean install

# Run Spring Boot application
mvn spring-boot:run

# Run tests
mvn test

# Skip tests during build
mvn clean install -DskipTests

# View dependencies
mvn dependency:tree

# Check for dependency updates
mvn versions:display-dependency-updates
```

---

## 📊 Testing Scenarios

### Scenario 1: Single Order

```bash
# Send one order
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Item1"],
    "amount": 100.00,
    "paymentDateTime": "2024-04-19T10:00:00"
  }'

# Verify in Kafka
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --max-messages 1
```

### Scenario 2: Bulk Orders

```bash
# Create 5 orders
for i in {1..5}; do
  curl -X POST http://localhost:8080/app/publish \
    -H "Content-Type: application/json" \
    -d "{
      \"products\": [\"Product$i\"],
      \"amount\": $((100 + i * 50)).50,
      \"paymentDateTime\": \"2024-04-19T1$((i+9)):00:00\"
    }"
  echo "Order $i created"
  sleep 1
done

# Count messages in topic
docker exec -it kafka kafka-run-class.sh \
  kafka.tools.JmxTool \
  --object-name kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec \
  --attributes OneMinuteRate
```

### Scenario 3: Check Idempotency

```bash
# Create an order
ORDER_DATA='{
  "products": ["Laptop"],
  "amount": 999.99,
  "paymentDateTime": "2024-04-19T10:30:00"
}'

# Send same order twice
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d "$ORDER_DATA"

curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d "$ORDER_DATA"

# Check database for duplicate
# Query: SELECT * FROM orders WHERE products = 'Laptop' AND amount = 999.99
# Should have only 1 entry (idempotency working)
```

---

## 🔍 Monitoring

### Kafka UI

Open browser: **http://localhost:8888**

Features:
- View all topics and partitions
- Monitor consumer groups and lag
- Produce/consume messages via UI
- View topic configuration

### REST Endpoints

```bash
# Order Service (Producer)
curl http://localhost:8080/swagger-ui.html          # Swagger API docs
curl http://localhost:8080/h2                        # H2 Database UI

# Health checks
curl http://localhost:8080/actuator/health
```

### View Logs

```bash
# Producer logs (running in foreground)
# See output in the terminal where mvn spring-boot:run is running

# Kafka logs
docker logs kafka

# Zookeeper logs
docker logs zookeeper

# Kafka UI logs
docker logs kafka-ui
```

### Database Queries

```bash
# Access H2 console
# Browser: http://localhost:8080/h2

# SQL Queries
# View all orders
SELECT * FROM orders;

# View processed orders (idempotency tracking)
SELECT * FROM processed_orders;

# Count by status
SELECT COUNT(*) as total_orders FROM orders;
```

---

## ⚙️ Configuration Customization

### Change Kafka Topic Name

**File**: `orderService/src/main/resources/application.yaml`
```yaml
spring:
  kafka:
    topic:
      name: my-custom-topic  # Change this
```

### Change Producer Port

**File**: `orderService/src/main/resources/application.yaml`
```yaml
server:
  port: 8090  # Change from 8080
```

### Change Kafka Broker

**File**: `orderService/src/main/resources/application.yaml`
```yaml
spring:
  kafka:
    bootstrap-servers: kafka.example.com:9092  # Change this
```

### Tune Producer Settings

**File**: `orderService/src/main/resources/application.yaml`
```yaml
spring:
  kafka:
    producer:
      retries: 5              # Increase retries
      linger.ms: 20          # Increase batch wait time
      batch.size: 32768      # Increase batch size
```

---

## 🐛 Troubleshooting

### Connection Refused

```bash
# Check if Kafka is running
docker-compose ps

# If not running, start it
docker-compose up -d

# Check logs
docker logs kafka
```

### Topic Not Found

```bash
# Topic may not be created yet
# Create it manually
docker exec -it kafka kafka-topics.sh \
  --create \
  --topic order-events \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

### No Messages in Topic

```bash
# Check if producer is sending
# Look for "Order published successfully" in response

# If OK, check consumer
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --timeout-ms 5000
```

### Consumer Lag High

```bash
# Check consumer lag
docker exec -it kafka kafka-consumer-groups.sh \
  --describe \
  --group notification-service-group \
  --bootstrap-server localhost:9092

# If LAG column shows high values, start consumer service
cd notificationService
mvn spring-boot:run
```

### Database File Locked

```bash
# H2 database might be locked
# Clear H2 data and restart
rm -rf orderService/h2.mv.db
docker-compose restart
```

---

## 📞 Quick Help

| Issue | Command |
|-------|---------|
| Start everything | `docker-compose up -d && cd orderService && mvn spring-boot:run` |
| Stop everything | `docker-compose down` |
| View Kafka messages | `docker exec -it kafka kafka-console-consumer.sh --topic order-events --from-beginning --bootstrap-server localhost:9092` |
| Check service health | `curl http://localhost:8080/actuator/health` |
| View database | `http://localhost:8080/h2` (JDBC: `jdbc:h2:mem:test`) |
| View Kafka UI | `http://localhost:8888` |
| Create topic | `docker exec -it kafka kafka-topics.sh --create --topic test --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092` |
| Reset consumer offset | `docker exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-group --reset-offsets --to-earliest --execute` |

---

## 📚 File Locations

```
/Users/karthikkollapureddy/Documents/Spring-boot-with-kafka/

├── orderService/                          # Producer service
│   ├── pom.xml
│   ├── src/main/java/...
│   └── src/main/resources/application.yaml
│
├── notificationService/                   # Consumer service (TODO)
│   └── pom.xml (create)
│
├── docker-compose.yml                     # Kafka setup
├── KAFKA_DEMO_README.md                   # Full documentation
├── CONSUMER_IMPLEMENTATION_GUIDE.md        # Consumer how-to
├── QUICKSTART_COMMANDS.md                 # This file
└── .gitignore
```

---

## 🎯 Next Steps

1. ✅ Start Kafka with `docker-compose up -d`
2. ✅ Run producer: `cd orderService && mvn spring-boot:run`
3. ✅ Create orders via REST API
4. ✅ Verify messages in Kafka
5. 📋 Implement consumer service (see CONSUMER_IMPLEMENTATION_GUIDE.md)
6. 🚀 Deploy to production

---

**Last Updated**: April 19, 2026  
**For detailed info**: See KAFKA_DEMO_README.md
