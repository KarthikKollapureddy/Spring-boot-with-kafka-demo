# Consumer Service Implementation Guide

A detailed step-by-step guide to implement the **Notification Service** (Kafka Consumer) for the Spring Boot with Kafka demo project.

---

## Table of Contents

1. [Quick Setup](#quick-setup)
2. [Detailed Implementation Steps](#detailed-implementation-steps)
3. [Code Templates](#code-templates)
4. [Configuration Reference](#configuration-reference)
5. [Testing the Consumer](#testing-the-consumer)
6. [Troubleshooting](#troubleshooting)

---

## Quick Setup

### Create Project Structure

```bash
cd /Users/karthikkollapureddy/Documents/Spring-boot-with-kafka

# Create notification service directory
mkdir -p notificationService/src/main/{java,resources}
mkdir -p notificationService/src/test/{java,resources}
```

### Create `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.kafkaConsumer</groupId>
    <artifactId>notificationService</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Notification Service</name>
    <description>Kafka Consumer for Order Notifications</description>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-kafka</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
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
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Detailed Implementation Steps

### Step 1: Create the Main Application Class

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/NotificationServiceApplication.java`

```java
package com.kafkaConsumer.notificationService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

### Step 2: Create Event Model (Same as Producer)

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/event/OrderEvent.java`

```java
package com.kafkaConsumer.notificationService.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message event consumed from Kafka topic.
 * Must match the producer's OrderEvent structure.
 */
public record OrderEvent(
    Long id,
    List<String> products,
    Float amount,
    LocalDateTime paymentDateTime
) {}
```

### Step 3: Create Notification Entity

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/entity/Notification.java`

```java
package com.kafkaConsumer.notificationService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long orderId;
    
    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING, RETRYING
    
    @Column(length = 500)
    private String errorMessage;
    
    @Column(length = 255)
    private String recipientEmail;
    
    @Column(nullable = false)
    private LocalDateTime sentAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt;
}
```

### Step 4: Create Repository

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/repository/NotificationRepository.java`

```java
package com.kafkaConsumer.notificationService.repository;

import com.kafkaConsumer.notificationService.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Check if notification already sent for order (idempotency)
     */
    boolean existsByOrderId(Long orderId);
    
    /**
     * Find notification by order ID
     */
    Optional<Notification> findByOrderId(Long orderId);
}
```

### Step 5: Create Notification Service (Business Logic)

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/service/NotificationService.java`

```java
package com.kafkaConsumer.notificationService.service;

import com.kafkaConsumer.notificationService.entity.Notification;
import com.kafkaConsumer.notificationService.event.OrderEvent;
import com.kafkaConsumer.notificationService.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class NotificationService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    /**
     * Send order notification email.
     * Includes idempotency check to prevent duplicate emails.
     */
    @Transactional
    public void sendOrderNotification(OrderEvent event) {
        // Check if already processed (idempotency)
        if (notificationRepository.existsByOrderId(event.id())) {
            log.warn("Notification already sent for order {}. Skipping...", event.id());
            return;
        }
        
        try {
            log.info("Sending notification for order {}", event.id());
            
            // Create email message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("customer@example.com"); // TODO: Add email to OrderEvent
            message.setSubject("Order Confirmation #" + event.id());
            message.setText(buildEmailBody(event));
            message.setFrom("orders@company.com");
            
            // Send email
            mailSender.send(message);
            
            // Log success in database
            Notification notification = Notification.builder()
                .orderId(event.id())
                .status("SUCCESS")
                .recipientEmail("customer@example.com")
                .sentAt(LocalDateTime.now())
                .build();
            
            notificationRepository.save(notification);
            
            log.info("Successfully sent notification for order {}", event.id());
            
        } catch (MailException e) {
            log.error("Failed to send email for order {}: {}", event.id(), e.getMessage());
            
            // Log failure in database
            Notification notification = Notification.builder()
                .orderId(event.id())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .sentAt(LocalDateTime.now())
                .build();
            
            notificationRepository.save(notification);
            
            // Re-throw to prevent offset commit
            throw new RuntimeException("Email send failed for order " + event.id(), e);
        }
    }
    
    /**
     * Build email body from order event
     */
    private String buildEmailBody(OrderEvent event) {
        return String.format(
            "Dear Customer,\n\n" +
            "Thank you for your order!\n\n" +
            "Order ID: %d\n" +
            "Products: %s\n" +
            "Amount: $%.2f\n" +
            "Payment Date: %s\n\n" +
            "Your order has been processed and will be shipped soon.\n" +
            "You will receive a shipping confirmation email shortly.\n\n" +
            "Thank you for your business!\n\n" +
            "Best regards,\n" +
            "The Order Team",
            event.id(),
            String.join(", ", event.products()),
            event.amount(),
            event.paymentDateTime()
        );
    }
}
```

### Step 6: Create Kafka Consumer Listener

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/service/OrderEventConsumer.java`

```java
package com.kafkaConsumer.notificationService.service;

import com.kafkaConsumer.notificationService.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for OrderEvent messages
 * Uses manual acknowledgment for guaranteed processing
 */
@Component
@Slf4j
public class OrderEventConsumer {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Main message consumer method.
     * Triggered when message arrives on 'order-events' topic.
     */
    @KafkaListener(
        topics = "order-events",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
        @Payload OrderEvent event,
        Acknowledgment ack,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received order event: {} (Partition: {}, Offset: {})", 
            event.id(), partition, offset);
        
        try {
            // Process the order event (send email, log, etc.)
            notificationService.sendOrderNotification(event);
            
            // Commit offset only after successful processing
            ack.acknowledge();
            log.info("Successfully processed and acknowledged order {}", event.id());
            
        } catch (Exception e) {
            log.error("Error processing order {}: {}", event.id(), e.getMessage(), e);
            // Don't acknowledge - message will be redelivered to consumer group
            // The same message will be processed again (at-least-once semantics)
        }
    }
}
```

### Step 7: Create Kafka Consumer Configuration

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/config/KafkaConsumerConfig.java`

```java
package com.kafkaConsumer.notificationService.config;

import com.kafkaConsumer.notificationService.event.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    /**
     * Configure Kafka consumer properties
     */
    @Bean
    public ConsumerFactory<Long, OrderEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Broker connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Key deserializer (Long orderId)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        
        // Value deserializer (OrderEvent)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Consumer behavior
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);      // Max records per poll
        
        // Fetch settings
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    /**
     * Configure listener container factory with manual acknowledgment
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, OrderEvent> 
        kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<Long, OrderEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Manual acknowledgment - must explicitly call ack.acknowledge()
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // Concurrency - number of threads processing messages
        factory.setConcurrency(1); // Can increase for parallel processing
        
        // Error handling
        factory.setCommonErrorHandler(kafkaErrorHandler());
        
        return factory;
    }
    
    /**
     * Global error handler for processing errors
     */
    @Bean
    public org.springframework.kafka.listener.CommonErrorHandler kafkaErrorHandler() {
        // Simple error handler - just log the error
        return new org.springframework.kafka.listener.DefaultErrorHandler();
    }
}
```

### Step 8: Create REST Controller (Status Endpoints)

**File**: `notificationService/src/main/java/com/kafkaConsumer/notificationService/controller/NotificationController.java`

```java
package com.kafkaConsumer.notificationService.controller;

import com.kafkaConsumer.notificationService.entity.Notification;
import com.kafkaConsumer.notificationService.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
    
    /**
     * Get notification status by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getNotificationStatus(@PathVariable Long orderId) {
        Optional<Notification> notification = notificationRepository.findByOrderId(orderId);
        
        if (notification.isPresent()) {
            return ResponseEntity.ok(notification.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all notifications (for debugging)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }
    
    /**
     * Get notifications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Notification>> getNotificationsByStatus(
        @PathVariable String status) {
        
        List<Notification> notifications = notificationRepository.findAll()
            .stream()
            .filter(n -> n.getStatus().equals(status))
            .toList();
        
        return ResponseEntity.ok(notifications);
    }
}
```

### Step 9: Create Application Configuration

**File**: `notificationService/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: notificationService
  
  # JPA Configuration
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: create-drop
  
  # Database Configuration (H2)
  datasource:
    username: sa
    password: sa
    url: jdbc:h2:mem:notification
  
  h2:
    console:
      enabled: true
      path: /h2
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest  # Start from beginning if group is new
      key-deserializer: org.apache.kafka.common.serialization.LongDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      
      properties:
        # Map JSON to OrderEvent class
        spring.json.type.mapping: orderEvent:com.kafkaConsumer.notificationService.event.OrderEvent
        spring.json.trusted.packages: "*"
      
      # Consumer behavior
      enable-auto-commit: false  # Manual acknowledgment
      fetch-min-bytes: 1
      fetch-max-wait-ms: 1000
      max-poll-records: 10
      session-timeout-ms: 30000
  
  # Mail Configuration (Gmail example)
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            protocols: TLSv1.2

# Server Configuration
server:
  port: 8081
  servlet:
    context-path: /notification-service

# Logging
logging:
  level:
    root: INFO
    com.kafkaConsumer: DEBUG
    org.springframework.kafka: DEBUG
    org.springframework.mail: DEBUG

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
```

---

## Configuration Reference

### Kafka Consumer Properties

| Property | Value | Purpose |
|----------|-------|---------|
| `bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `group-id` | `notification-service-group` | Consumer group identifier |
| `auto-offset-reset` | `earliest` | Read from beginning for new groups |
| `enable-auto-commit` | `false` | Require manual offset commitment |
| `max-poll-records` | `10` | Fetch max 10 records per poll |

### Mail Configuration

| Property | Value | Purpose |
|----------|-------|---------|
| `host` | `smtp.gmail.com` | SMTP server host |
| `port` | `587` | SMTP port (TLS) |
| `username` | `your-email@gmail.com` | Email account |
| `password` | `app-password` | App-specific password |

**Note**: For Gmail, use an **App Password**, not your account password. Enable 2-factor authentication first.

---

## Testing the Consumer

### Test 1: Start Consumer Service

```bash
cd notificationService
mvn spring-boot:run
```

Expected output:
```
Started NotificationServiceApplication in 3.5 seconds
```

### Test 2: Verify Consumer is Connected

```bash
# Check Kafka consumer group
docker exec -it kafka kafka-consumer-groups.sh \
  --describe \
  --group notification-service-group \
  --bootstrap-server localhost:9092
```

### Test 3: Send an Order (Trigger Consumer)

In a separate terminal, create an order:

```bash
curl -X POST http://localhost:8080/app/publish \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["Laptop", "Mouse"],
    "amount": 1500.00,
    "paymentDateTime": "2024-04-19T14:30:00"
  }'
```

### Test 4: Check Notification Status

```bash
# Get notification for order ID 1
curl http://localhost:8081/api/notifications/order/1

# Get all notifications
curl http://localhost:8081/api/notifications/all

# Get notifications by status
curl http://localhost:8081/api/notifications/status/SUCCESS
```

### Test 5: Monitor Consumer Logs

```bash
# Watch logs in real-time
tail -f logs/notification-service.log | grep -i "order"
```

### Test 6: Verify Email Would Be Sent (Without Real SMTP)

Currently configured for real Gmail SMTP. For testing without sending real emails:

```yaml
# Test configuration (application-test.yaml)
spring:
  mail:
    # Use mock implementation for testing
    host: localhost
    port: 1025  # Mailhog mock SMTP port
```

Or use **MailHog** Docker container:

```bash
docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog

# Then set in configuration:
spring.mail.host: localhost
spring.mail.port: 1025
```

---

## Troubleshooting

### Consumer Not Receiving Messages

```bash
# Check consumer group lag
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --describe

# If LAG > 0, consumer is behind
# Check consumer service logs for errors
```

### Messages Not Being Committed

```bash
# Verify enable-auto-commit is false
# Verify ack.acknowledge() is being called in OrderEventConsumer

# Check offset position
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --reset-offsets \
  --to-earliest \
  --execute
```

### Duplicate Notifications Sent

```sql
-- Check for duplicate entries in database
SELECT order_id, COUNT(*) FROM notifications GROUP BY order_id HAVING COUNT(*) > 1;

-- Verify notificationRepository.existsByOrderId() is being called
-- Check that transaction boundaries are correct
```

### Email Not Sending

```bash
# Verify SMTP credentials are correct
# Check that app password (not account password) is used for Gmail
# Enable "Less secure app access" if needed

# Test SMTP connection
docker exec -it notification-service bash
telnet smtp.gmail.com 587

# View email logs
grep -i "mail\|smtp" logs/notification-service.log
```

### High Consumer Lag

```bash
# Increase consumer instances for parallel processing
# Or reduce processing time (optimize email sending)
# Or increase batch size (max-poll-records)

# Monitor with metrics
curl http://localhost:8081/actuator/metrics/kafka.consumer.records.lag
```

---

## Summary

You now have a complete guide to implement the Notification Service consumer. Follow the steps in order:

1. ✅ Create project structure
2. ✅ Create `pom.xml`
3. ✅ Create application class
4. ✅ Create event model
5. ✅ Create JPA entities and repositories
6. ✅ Create service classes
7. ✅ Create consumer listener
8. ✅ Create configuration
9. ✅ Create REST controller
10. ✅ Configure application.yaml

Then test by creating orders and verifying notifications are processed.

**Happy coding!**
