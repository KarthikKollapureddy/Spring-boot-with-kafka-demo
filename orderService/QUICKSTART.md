# Quick Start Guide

**Get the Order Service running in 5 minutes**

---

## 1️⃣ Prerequisites

```bash
# Check Java 21 is installed
java -version
# Output should show: openjdk version "21.x.x"

# Check Maven is installed
mvn -version
# Output should show: Apache Maven 3.x.x
```

---

## 2️⃣ Start Kafka (Local Development)

### Option A: Docker (Easiest)
```bash
# Start Kafka + Zookeeper with Docker Compose
docker-compose up -d

# Verify Kafka is running
docker ps | grep kafka
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### Option B: Manual (From Kafka Tarball)
```bash
# Download Kafka 4.1.2
cd ~/Downloads
wget https://archive.apache.org/dist/kafka/4.1.2/kafka_2.13-4.1.2.tgz
tar -xzf kafka_2.13-4.1.2.tgz
cd kafka_2.13-4.1.2

# Terminal 1: Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Terminal 2: Start Kafka Broker
bin/kafka-server-start.sh config/server.properties

# Terminal 3: Create topic (optional)
bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --partitions 1 \
  --replication-factor 1
```

---

## 3️⃣ Build the Application

```bash
# Navigate to project
cd ~/Documents/Spring-boot-with-kafka/orderService

# Clean and build
mvn clean install -DskipTests

# Output should end with:
# BUILD SUCCESS
```

---

## 4️⃣ Run the Application

```bash
# Start the service
mvn spring-boot:run

# Or using JAR (after building):
java -jar target/orderService-0.0.1-SNAPSHOT.jar

# Wait for log:
# Started OrderServiceApplication in X.XXX seconds
```

---

## 5️⃣ Test the API

### Using cURL
```bash
# Create an order
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["apple", "banana"],
    "amount": 28.1,
    "orderProcessingDateTime": "2026-04-19T02:00:55.439+05:30"
  }'

# Expected response:
# {"orderId":1,"message":"Successfully published message"}
```

### Using Postman
1. **New → Request**
2. **Method:** POST
3. **URL:** `http://localhost:8080/app/publish`
4. **Headers:** `Content-Type: application/json`
5. **Body (raw JSON):**
```json
{
  "products": ["laptop", "mouse"],
  "amount": 1500.00,
  "orderProcessingDateTime": "2026-04-19T14:30:00.000Z"
}
```
6. **Send**

### Expected Success Response
```json
HTTP/1.1 200 OK

{
  "orderId": 1,
  "message": "Successfully published message"
}
```

---

## 📊 Verify Everything Works

### Check Application Logs
```bash
# Look for these log entries:

# 1. Order processing started
🔔 Sending OrderEvent to Kafka topic: order-events - OrderID: 1

# 2. Kafka producer initialized
[Producer clientId=orderService-producer-1] Instantiated an idempotent producer.

# 3. Message successfully published
✅ Successfully published OrderEvent to topic: order-events - 
   OrderID: 1, Partition: 0, Offset: 0, Timestamp: ...
```

### Access H2 Database Console
```
URL: http://localhost:8080/h2
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:mem:test
User Name: sa
Password: sa
```

**Query to see orders:**
```sql
SELECT * FROM orders_info;
SELECT * FROM processed_orders;
```

---

## 🛠️ Troubleshooting

### Issue: "Connection refused - localhost:9092"
**Solution:** Kafka is not running
```bash
# Check if Kafka is running
lsof -i :9092

# If not running, start Kafka (see Step 2)
```

### Issue: "MethodArgumentNotValidException: type mismatch"
**Solution:** DateTime format is wrong
```json
// ❌ Wrong
{"orderProcessingDateTime": "2026-04-19"}

// ✅ Correct (with time)
{"orderProcessingDateTime": "2026-04-19T14:30:00.000Z"}

// ✅ Also correct (with timezone)
{"orderProcessingDateTime": "2026-04-19T02:00:55.439+05:30"}
```

### Issue: "Failed to connect to database"
**Solution:** H2 driver issue or schema not created
```bash
# Check logs for:
# [H2 Console Server] Server created
# [H2 Tcp Server] Server started

# Restart application:
mvn clean spring-boot:run
```

### Issue: "Topic 'order-events' does not exist"
**Solution:** Create the topic manually or let producer auto-create it
```bash
# Option 1: Auto-create (producer creates on first message)
# Just send a request, topic will be created

# Option 2: Manual creation
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-events
```

---

## 📈 Load Testing (Optional)

### Using Apache Bench
```bash
# Install if needed
brew install ab

# Simple load test: 100 requests, 10 concurrent
ab -n 100 -c 10 -p order.json -T application/json \
  http://localhost:8080/app/publish

# where order.json contains:
# {
#   "products": ["item1", "item2"],
#   "amount": 100.0,
#   "orderProcessingDateTime": "2026-04-19T12:00:00.000Z"
# }
```

### Using JMeter
```bash
# Download from: https://jmeter.apache.org/
# Create test plan with HTTP Request sampler
# Add listeners for throughput, latency analysis
```

---

## 🧹 Cleanup

### Stop Application
```bash
# Press Ctrl+C in terminal running the app
```

### Stop Kafka
```bash
# If using Docker:
docker-compose down

# If using manual installation:
# Press Ctrl+C in Kafka terminal
# Press Ctrl+C in Zookeeper terminal
```

### Clean Project
```bash
# Remove build artifacts
mvn clean

# Full reset
rm -rf target/
mvn clean install
```

---

## ✅ Checklist

- [ ] Java 21 installed and verified
- [ ] Maven installed and verified
- [ ] Kafka running on localhost:9092
- [ ] Application built successfully (`mvn clean install`)
- [ ] Application started (`mvn spring-boot:run`)
- [ ] API endpoint responds to POST request
- [ ] Kafka logs show successful message publishing
- [ ] H2 console accessible at http://localhost:8080/h2
- [ ] Orders table has records

---

## 🎯 Next Steps

1. **Explore the Code**
   - Read `README.md` for architecture overview
   - Review `RATING.md` for code quality assessment
   - Study `OrderProcessorImpl.java` for business logic

2. **Try Advanced Scenarios**
   - Send multiple orders and verify they get unique IDs
   - Check H2 database to see persisted data
   - Review Kafka logs for producer behavior

3. **Enhance the Application**
   - Add unit tests (see RATING.md recommendations)
   - Implement Resilience4j for fault tolerance
   - Add Dead Letter Topic handling

---

## 📞 Common Commands

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests (when added)
mvn test

# Package
mvn package

# Check Kafka topic
kafka-topics.sh --list --bootstrap-server localhost:9092

# Consume messages from topic (in new terminal)
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning

# View application logs with filtering
mvn spring-boot:run | grep "OrderEvent\|Successfully"
```

---

**Ready? Start with Step 2 and have fun! 🚀**
