# Kafka Producer-Consumer Demo - Complete Documentation Index

Welcome to the **Spring Boot with Kafka** project! This is a comprehensive demonstration of a real-world Kafka producer-consumer architecture for order processing and email notifications.

---

## 📚 Documentation Overview

### For New Users (Start Here!)

1. **[QUICKSTART_COMMANDS.md](./QUICKSTART_COMMANDS.md)** ⭐ **START HERE**
   - 5-minute setup guide
   - Essential commands reference
   - Quick troubleshooting
   - **Time required**: 5-10 minutes

### Architecture & Design

2. **[KAFKA_DEMO_README.md](./KAFKA_DEMO_README.md)** 🏗️ **COMPREHENSIVE GUIDE**
   - Complete system architecture with diagrams
   - Producer service documentation (✅ Ready)
   - Consumer service specification (📋 TODO)
   - Kafka configuration details
   - Production considerations
   - **Time required**: 30-45 minutes

### Implementation Guides

3. **[CONSUMER_IMPLEMENTATION_GUIDE.md](./CONSUMER_IMPLEMENTATION_GUIDE.md)** 📋 **FOR DEVELOPERS**
   - Step-by-step consumer service implementation
   - Complete code templates with explanations
   - Configuration reference
   - Testing procedures
   - Troubleshooting guide
   - **Time required**: 2-3 hours (implementation)

4. **[PRODUCER_API_DOCS.md](./PRODUCER_API_DOCS.md)** 📤 **API REFERENCE**
   - REST API endpoint specifications
   - Request/response formats with examples
   - Kafka message specifications
   - Error handling guide
   - Performance characteristics
   - **Time required**: 15-20 minutes

---

## 🚀 Quick Start (5 Minutes)

```bash
# 1. Start Kafka
cd /Users/karthikkollapureddy/Documents/Spring-boot-with-kafka
docker-compose up -d

# 2. Start Producer (Order Service)
cd orderService
mvn spring-boot:run

# 3. Create an order (in new terminal)
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop"],
    "amount": 999.99,
    "paymentDateTime": "2024-04-19T10:30:00"
  }'

# 4. Verify in Kafka
docker exec -it kafka kafka-console-consumer.sh \
  --topic order-events \
  --from-beginning \
  --bootstrap-server localhost:9092
```

✅ **You're done!** Producer is working.

---

## 📊 Project Structure

```
Spring-boot-with-kafka/
│
├── 📄 README FILES (Start with these!)
│   ├── KAFKA_DEMO_README.md              # Full architecture & design
│   ├── CONSUMER_IMPLEMENTATION_GUIDE.md  # How to build the consumer
│   ├── PRODUCER_API_DOCS.md              # API reference
│   ├── QUICKSTART_COMMANDS.md            # Commands cheatsheet
│   └── INDEX.md                          # This file
│
├── 🔧 CONFIGURATION
│   ├── docker-compose.yml                # Kafka + Zookeeper setup
│   ├── .gitignore                        # Git ignore rules
│   
├── 📦 orderService/                      # PRODUCER (✅ Ready)
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kafkaProducer/orderService/
│   │   │   │   ├── OrderServiceApplication.java
│   │   │   │   ├── controller/OrdersController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── OrderProcessor.java
│   │   │   │   │   ├── OrderProcessorImpl.java
│   │   │   │   │   └── OrderProducer.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Orders.java
│   │   │   │   │   └── ProcessedOrders.java
│   │   │   │   ├── event/OrderEvent.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── OrderRequestDTO.java
│   │   │   │   │   └── OrderResponseDTO.java
│   │   │   │   └── repository/
│   │   │   │       ├── OrdersRepository.java
│   │   │   │       └── ProcessedOrderRepository.java
│   │   │   └── resources/
│   │   │       └── application.yaml
│   │   └── test/
│   └── target/
│
└── 📦 notificationService/               # CONSUMER (📋 TODO)
    ├── pom.xml (create)
    └── src/
        ├── main/java/com/kafkaConsumer/notificationService/
        │   ├── NotificationServiceApplication.java
        │   ├── config/KafkaConsumerConfig.java
        │   ├── service/
        │   │   ├── OrderEventConsumer.java
        │   │   └── NotificationService.java
        │   ├── entity/Notification.java
        │   ├── event/OrderEvent.java
        │   └── repository/NotificationRepository.java
        └── resources/application.yaml
```

---

## 📖 Which File to Read?

### I want to...

| Goal | Read This | Time |
|------|-----------|------|
| Get started immediately | [QUICKSTART_COMMANDS.md](./QUICKSTART_COMMANDS.md) | 5 min |
| Understand the architecture | [KAFKA_DEMO_README.md](./KAFKA_DEMO_README.md) | 30 min |
| Build the consumer service | [CONSUMER_IMPLEMENTATION_GUIDE.md](./CONSUMER_IMPLEMENTATION_GUIDE.md) | 2-3 hrs |
| Call the REST API | [PRODUCER_API_DOCS.md](./PRODUCER_API_DOCS.md) | 10 min |
| Find a command | [QUICKSTART_COMMANDS.md](./QUICKSTART_COMMANDS.md) - "Common Commands" | 2 min |
| Debug an error | [KAFKA_DEMO_README.md](./KAFKA_DEMO_README.md) - "Troubleshooting" | 5-10 min |

---

## 🎯 Learning Path

### Beginner (Never used Kafka before)

```
Week 1:
  Day 1: Read KAFKA_DEMO_README.md (System Architecture section)
  Day 2: Run QUICKSTART_COMMANDS.md (5-minute setup)
  Day 3: Explore PRODUCER_API_DOCS.md (test the API)
  Day 4-7: Review existing producer code and understand patterns

Week 2:
  Day 1-3: Read CONSUMER_IMPLEMENTATION_GUIDE.md
  Day 4-7: Implement notification service following the guide
```

### Intermediate (Some Kafka experience)

```
Day 1: Skim KAFKA_DEMO_README.md
Day 2: Run QUICKSTART_COMMANDS.md
Day 3: Use CONSUMER_IMPLEMENTATION_GUIDE.md as reference
Day 4: Customize and extend with your own logic
```

### Advanced (Kafka expert)

```
- Review existing code directly
- Customize configuration as needed
- Add additional features/improvements
- Deploy to production infrastructure
```

---

## 🔑 Key Concepts

### Producer (Order Service) ✅ READY

| Concept | File | Explanation |
|---------|------|-------------|
| REST API | PRODUCER_API_DOCS.md | HTTP endpoint for creating orders |
| OrderEvent | orderService/.../event/OrderEvent.java | Message structure (Java record) |
| Kafka Publishing | orderService/.../service/OrderProducer.java | KafkaTemplate implementation |
| Idempotency | orderService/.../entity/ProcessedOrders.java | Prevent duplicate orders |
| Configuration | orderService/.../resources/application.yaml | Kafka producer settings |

### Consumer (Notification Service) 📋 TODO

| Concept | File | Explanation |
|---------|------|-------------|
| Kafka Listener | CONSUMER_IMPLEMENTATION_GUIDE.md | @KafkaListener annotation |
| Email Sending | CONSUMER_IMPLEMENTATION_GUIDE.md | JavaMailSender integration |
| Manual Acknowledgment | CONSUMER_IMPLEMENTATION_GUIDE.md | Explicit offset management |
| Error Handling | KAFKA_DEMO_README.md | DLQ & retry logic |
| Configuration | CONSUMER_IMPLEMENTATION_GUIDE.md | Kafka consumer settings |

---

## ⚙️ Technology Stack

- **Java**: 21
- **Spring Boot**: 4.0.5
- **Spring Kafka**: Latest
- **Apache Kafka**: 7.6.0
- **Zookeeper**: 7.6.0
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Build Tool**: Maven
- **Container**: Docker & Docker Compose

---

## 🔗 Important Ports

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| Order Service | 8080 | http://localhost:8080 | REST API & H2 console |
| Kafka Broker | 9092 | localhost:9092 | Kafka connections |
| Zookeeper | 2181 | localhost:2181 | Kafka coordination |
| Kafka UI | 8888 | http://localhost:8888 | Visual Kafka management |
| Notification Service | 8081 | http://localhost:8081 | Consumer service (TODO) |

---

## 📋 Implementation Checklist

### Phase 1: Producer (✅ Complete)

- [x] Order Service REST API
- [x] Kafka Producer configuration
- [x] H2 Database integration
- [x] Idempotency tracking
- [x] Error handling & retries
- [x] API documentation

### Phase 2: Consumer (📋 TODO)

- [ ] Create NotificationService project
- [ ] Set up Kafka Consumer config
- [ ] Implement OrderEventConsumer listener
- [ ] Create Notification entity & repository
- [ ] Implement email sending
- [ ] Add manual acknowledgment
- [ ] Configure SMTP
- [ ] Add error handling & DLQ
- [ ] Create REST endpoints for status
- [ ] Test end-to-end flow

### Phase 3: Production (🚀 Future)

- [ ] Add authentication (OAuth2/JWT)
- [ ] Configure message encryption
- [ ] Add monitoring (Prometheus/Grafana)
- [ ] Set up alerting
- [ ] Create deployment scripts
- [ ] Load testing
- [ ] Security hardening
- [ ] Documentation updates

---

## 🧪 Testing Checklist

### Producer Testing

- [ ] Can create single order
- [ ] Can create bulk orders
- [ ] Idempotency works (same order sent twice = 1 entry)
- [ ] Messages appear in Kafka topic
- [ ] Error responses are correct
- [ ] Database persists correctly

### Consumer Testing (TODO)

- [ ] Can consume messages from Kafka
- [ ] Sends email notifications
- [ ] Handles errors gracefully
- [ ] Doesn't send duplicate emails
- [ ] DLQ captures failed messages
- [ ] Offset management works correctly

### End-to-End Testing

- [ ] Create order via REST API
- [ ] Verify message in Kafka
- [ ] Consumer processes message
- [ ] Email is sent (when implemented)
- [ ] Notification logged in database
- [ ] No duplicates on retry

---

## 🐛 Troubleshooting Quick Links

| Issue | See | Command |
|-------|-----|---------|
| Kafka won't start | QUICKSTART_COMMANDS.md | `docker-compose logs kafka` |
| No messages in topic | KAFKA_DEMO_README.md - Troubleshooting | `docker exec -it kafka kafka-topics.sh --describe --topic order-events --bootstrap-server localhost:9092` |
| Producer error | PRODUCER_API_DOCS.md - Error Handling | Check HTTP response status |
| Connection refused | QUICKSTART_COMMANDS.md - Troubleshooting | `docker-compose ps` |
| High consumer lag | KAFKA_DEMO_README.md | Check consumer group with `kafka-consumer-groups.sh` |

---

## 📞 Support & Resources

### Internal Documentation
- Full architecture: See KAFKA_DEMO_README.md
- API examples: See PRODUCER_API_DOCS.md
- Implementation help: See CONSUMER_IMPLEMENTATION_GUIDE.md
- Commands: See QUICKSTART_COMMANDS.md

### External Resources
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Boot Kafka](https://spring.io/projects/spring-kafka)
- [Confluent Kafka Tutorials](https://www.confluent.io/blog/kafka-tutorials/)
- [Kafka Best Practices](https://www.confluent.io/blog/kafka-best-practices/)

---

## 📝 Summary

This project demonstrates a **production-ready Kafka architecture** with:

✅ **Producer**: Ready to accept orders and publish to Kafka  
📋 **Consumer**: TODO - Follow CONSUMER_IMPLEMENTATION_GUIDE.md  
🔧 **Infrastructure**: Docker Compose for local development  
📚 **Documentation**: Comprehensive guides and examples  

**Next Steps**:
1. Run QUICKSTART_COMMANDS.md to see it working
2. Read KAFKA_DEMO_README.md to understand the design
3. Follow CONSUMER_IMPLEMENTATION_GUIDE.md to build the consumer
4. Extend with your own features

---

## 📄 Document Versions

| Document | Version | Last Updated | Status |
|----------|---------|--------------|--------|
| KAFKA_DEMO_README.md | 1.0 | 2024-04-19 | Complete |
| CONSUMER_IMPLEMENTATION_GUIDE.md | 1.0 | 2024-04-19 | Complete |
| PRODUCER_API_DOCS.md | 1.0 | 2024-04-19 | Complete |
| QUICKSTART_COMMANDS.md | 1.0 | 2024-04-19 | Complete |
| INDEX.md | 1.0 | 2024-04-19 | This file |

---

**Created**: April 19, 2024  
**Project**: Spring Boot with Kafka - Producer-Consumer Demo  
**Status**: Production Ready (Producer), TODO (Consumer)  
**Maintainer**: Your Team

---

**Happy coding! 🚀**

For questions or improvements, refer to the relevant documentation file above or check the troubleshooting sections.
