# Producer API Documentation

Complete reference for the Order Service (Producer) REST API and Kafka message specifications.

---

## Table of Contents

1. [REST API Endpoints](#rest-api-endpoints)
2. [Request/Response Formats](#requestresponse-formats)
3. [Message Specifications](#message-specifications)
4. [Error Handling](#error-handling)
5. [Example Requests](#example-requests)
6. [Performance Characteristics](#performance-characteristics)

---

## REST API Endpoints

### Base URL

```
http://localhost:8080
```

### Health Check

```http
GET /actuator/health
Content-Type: application/json
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafkaProducer": {"status": "UP"},
    "livenessState": {"status": "UP"},
    "readinessState": {"status": "UP"}
  }
}
```

---

## Create Order

The main endpoint to create and publish orders to Kafka.

```http
POST /app/publish
Content-Type: application/json
```

### Request Body

```json
{
  "products": ["string"],
  "amount": "number",
  "paymentDateTime": "ISO-8601 DateTime"
}
```

### Field Descriptions

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `products` | Array[String] | ✅ Yes | List of product names | `["Laptop", "Mouse"]` |
| `amount` | Float/Double | ✅ Yes | Order total amount | `999.99` |
| `paymentDateTime` | LocalDateTime | ✅ Yes | Payment timestamp (ISO-8601) | `2024-04-19T10:30:00` |

### Response (200 OK)

```json
{
  "id": "number",
  "products": ["string"],
  "amount": "number",
  "paymentDateTime": "ISO-8601 DateTime",
  "message": "string"
}
```

### Response Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique order identifier (auto-generated) |
| `products` | Array[String] | Echo of request products |
| `amount` | Float | Echo of request amount |
| `paymentDateTime` | LocalDateTime | Echo of request datetime |
| `message` | String | Status message from server |

---

## Request/Response Formats

### Full Example Request

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": [
      "Dell XPS 13 Laptop",
      "Wireless Mouse",
      "USB-C Hub"
    ],
    "amount": 1299.99,
    "paymentDateTime": "2024-04-19T14:30:45"
  }'
```

### Full Example Response

```json
{
  "id": 5,
  "products": [
    "Dell XPS 13 Laptop",
    "Wireless Mouse",
    "USB-C Hub"
  ],
  "amount": 1299.99,
  "paymentDateTime": "2024-04-19T14:30:45",
  "message": "Order published successfully"
}
```

### DateTime Format

The API uses **ISO-8601 format** for datetime values:

```
YYYY-MM-DDTHH:mm:ss
YYYY-MM-DDTHH:mm:ss.SSS
```

**Valid Examples**:
- `2024-04-19T10:30:00` - Simple format
- `2024-04-19T10:30:00.500` - With milliseconds
- `2024-04-19T23:59:59` - End of day

**Invalid Examples** (will be rejected):
- `04/19/2024 10:30:00` - US format
- `2024-04-19 10:30:00` - Space instead of T
- `2024-04-19` - Date only

---

## Message Specifications

### Kafka Topic

- **Topic Name**: `order-events`
- **Partitions**: 3 (partitioned by `orderId`)
- **Replication Factor**: 3 (in production)
- **Retention**: 7 days (604800000 ms)

### Message Key

```
Long (orderId)
```

**Example**: `5` (for order ID 5)

### Message Value (OrderEvent)

```json
{
  "id": 5,
  "products": [
    "Laptop",
    "Mouse"
  ],
  "amount": 999.99,
  "paymentDateTime": "2024-04-19T10:30:00"
}
```

### Message Headers

| Header | Value | Purpose |
|--------|-------|---------|
| `correlation-id` | UUID | Trace request across systems |
| `timestamp` | Long | Message creation timestamp |

### Message Serialization

- **Key Serializer**: `LongSerializer` (Kafka binary format)
- **Value Serializer**: `JsonSerializer` (JSON text format)

### Example Kafka Message (Raw Format)

```
Topic: order-events
Partition: 0
Offset: 42
Key: 5
Value: {"id":5,"products":["Laptop","Mouse"],"amount":999.99,"paymentDateTime":"2024-04-19T10:30:00"}
Timestamp: 1713607800000
Headers: [correlation-id: 550e8400-e29b-41d4-a716-446655440000]
```

---

## Error Handling

### Error Response Format

```json
{
  "timestamp": "ISO-8601 DateTime",
  "status": "number",
  "error": "string",
  "message": "string",
  "path": "string"
}
```

### Common Errors

#### 400 Bad Request - Invalid JSON

```json
{
  "timestamp": "2024-04-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON parse error: Unrecognized field \"product\" (not \"products\")",
  "path": "/app/publish"
}
```

**Solution**: Check field names and JSON syntax.

#### 400 Bad Request - Missing Field

```json
{
  "timestamp": "2024-04-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Field 'amount' is required",
  "path": "/app/publish"
}
```

**Solution**: Ensure all required fields are present.

#### 400 Bad Request - Invalid DateTime Format

```json
{
  "timestamp": "2024-04-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid datetime format. Expected: YYYY-MM-DDTHH:mm:ss",
  "path": "/app/publish"
}
```

**Solution**: Use ISO-8601 format (2024-04-19T10:30:00).

#### 500 Internal Server Error - Kafka Publishing Failed

```json
{
  "timestamp": "2024-04-19T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to publish order to Kafka: Connection refused",
  "path": "/app/publish"
}
```

**Solution**: 
- Verify Kafka broker is running
- Check bootstrap-servers configuration
- Check network connectivity

#### 500 Internal Server Error - Database Error

```json
{
  "timestamp": "2024-04-19T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Database error: Unique constraint violation",
  "path": "/app/publish"
}
```

**Solution**: Clear H2 database and restart application.

---

## Example Requests

### Using cURL

#### Simple Order

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Book"],
    "amount": 29.99,
    "paymentDateTime": "2024-04-19T10:00:00"
  }'
```

#### Multiple Products

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Monitor", "Keyboard", "Mouse", "USB Hub"],
    "amount": 450.00,
    "paymentDateTime": "2024-04-19T14:30:00"
  }'
```

#### With Milliseconds

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Headphones"],
    "amount": 199.99,
    "paymentDateTime": "2024-04-19T10:30:45.250"
  }'
```

### Using Python

```python
import requests
import json
from datetime import datetime

url = "http://localhost:8080/app/publish"

payload = {
    "products": ["Laptop", "Mouse"],
    "amount": 999.99,
    "paymentDateTime": datetime.now().isoformat()
}

headers = {
    "Content-Type": "application/json"
}

response = requests.post(url, json=payload, headers=headers)
print(response.json())
```

### Using JavaScript/Node.js

```javascript
const axios = require('axios');

const url = 'http://localhost:8080/app/publish';

const payload = {
  products: ['Laptop', 'Mouse'],
  amount: 999.99,
  paymentDateTime: new Date().toISOString().split('.')[0]
};

axios.post(url, payload)
  .then(response => console.log(response.data))
  .catch(error => console.error(error));
```

### Using Java

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

RestTemplate restTemplate = new RestTemplate();

String url = "http://localhost:8080/app/publish";

Map<String, Object> request = new HashMap<>();
request.put("products", Arrays.asList("Laptop", "Mouse"));
request.put("amount", 999.99);
request.put("paymentDateTime", LocalDateTime.now().toString());

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

HttpEntity<Map> entity = new HttpEntity<>(request, headers);
Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

System.out.println(response);
```

### Using Postman

1. **Method**: POST
2. **URL**: `http://localhost:8080/app/publish`
3. **Headers**:
   - `Content-Type: application/json`
4. **Body** (JSON):
   ```json
   {
     "products": ["Laptop", "Mouse"],
     "amount": 999.99,
     "paymentDateTime": "2024-04-19T10:30:00"
   }
   ```
5. **Click**: Send

### Bulk Order Creation Script

```bash
#!/bin/bash

# Create 10 orders
for i in {1..10}; do
  AMOUNT=$((100 + i * 50))
  HOUR=$((10 + i / 4))
  MINUTE=$((i % 60))
  
  curl -X POST http://localhost:8080/app/publish \
    -H "Content-Type: application/json" \
    -d "{
      \"products\": [\"Product $i\"],
      \"amount\": $AMOUNT.99,
      \"paymentDateTime\": \"2024-04-19T${HOUR}:${MINUTE}:00\"
    }"
  
  echo "Created order $i"
  sleep 1  # Wait 1 second between requests
done
```

---

## Performance Characteristics

### Latency

| Operation | Typical Time | Notes |
|-----------|--------------|-------|
| REST request processing | 5-10 ms | Validation + DB save |
| Kafka publish | 10-50 ms | Network + broker acknowledgment |
| Total end-to-end | 20-80 ms | Varies with network |

### Throughput

- **Single thread**: ~100 orders/second
- **With batching**: ~500 orders/second
- **Network limited**: Depends on bandwidth

### Kafka Producer Tuning

Current configuration aims for **low-latency, reliable delivery**:

```yaml
spring.kafka.producer:
  linger.ms: 10         # Wait 10ms to batch
  batch.size: 16384     # Batch up to 16KB
  acks: all             # Wait for all replicas
  retries: 3            # Retry 3 times
```

**For higher throughput**, increase batching:

```yaml
spring.kafka.producer:
  linger.ms: 100        # Wait longer for batches
  batch.size: 32768     # Larger batches
  compression.type: snappy  # Compress messages
```

### Database Performance

- **H2 (in-memory)**: ~1000 inserts/second
- **PostgreSQL**: ~500 inserts/second
- **MySQL**: ~400 inserts/second

### Monitoring Metrics

```bash
# Check producer metrics
curl http://localhost:8080/actuator/metrics/kafka.producer.records.sent

# Check database metrics
curl http://localhost:8080/actuator/metrics/db.h2

# Get all metrics
curl http://localhost:8080/actuator/metrics
```

### Scaling Considerations

1. **Vertical Scaling**: Increase JVM heap size
   ```bash
   export JAVA_OPTS="-Xmx2g -Xms2g"
   mvn spring-boot:run
   ```

2. **Horizontal Scaling**: Run multiple producer instances
   - Use load balancer (Nginx, HAProxy)
   - Each instance publishes to same Kafka topic
   - Consumer group automatically distributes work

3. **Kafka Scaling**: Increase partitions
   ```bash
   docker exec -it kafka kafka-topics.sh \
     --alter \
     --topic order-events \
     --partitions 6 \
     --bootstrap-server localhost:9092
   ```

---

## Summary

The Order Service (Producer) provides a simple yet robust REST API for:
- ✅ Creating orders with validation
- ✅ Publishing to Kafka topic
- ✅ Persisting to H2 database
- ✅ Guaranteed delivery with retries
- ✅ Idempotency prevention

Use the examples above to integrate with any client application!

---

**Last Updated**: April 19, 2026  
**API Version**: 1.0  
**Status**: Production Ready
