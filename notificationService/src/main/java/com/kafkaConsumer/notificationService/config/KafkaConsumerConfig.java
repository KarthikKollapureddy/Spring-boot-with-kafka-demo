package com.kafkaConsumer.notificationService.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaConsumer.notificationService.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<Long, OrderEvent> consumerFactory(){
        Map<String, Object> props = new HashMap<>();
        // Broker config
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Key deserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);

        // Value deserializer (OrderEvent)
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderEventDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, OrderEvent> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<Long, OrderEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(consumerFactory());

        // Manual acknowledgment - must explicitly call ack.acknowledge()
        containerFactory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        // Concurrency - number of threads processing messages
        containerFactory.setConcurrency(1); // Can increase for parallel processing

        // Error handling
        containerFactory.setCommonErrorHandler(kafkaErrorHandler());

        return containerFactory;
    }

    @Bean
    public org.springframework.kafka.listener.CommonErrorHandler kafkaErrorHandler() {
        return new org.springframework.kafka.listener.DefaultErrorHandler();
    }
}

// Custom deserializer for OrderEvent using Jackson
class OrderEventDeserializer implements Deserializer<OrderEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public OrderEvent deserialize(String topic, byte[] data) {
        if (data == null){
            return null;
        } else {
            try {
                OrderEvent orderEvent = objectMapper.readValue(data, OrderEvent.class);
                return orderEvent;
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing OrderEvent", e);
            }
        }
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }

//    @Override
//    public void configure(java.util.Map<String, ?> configs, boolean isKey) {
//    }
//
//    @Override
//    public OrderEvent deserialize(String topic, byte[] data) {
//        if (data == null) {
//            return null;
//        }
//        try {
//            return objectMapper.readValue(data, OrderEvent.class);
//        } catch (Exception e) {
//            throw new RuntimeException("Error deserializing OrderEvent", e);
//        }
//    }
//
//    @Override
//    public void close() {
//    }
}

