package com.kafkaConsumer.notificationService.service;

import com.kafkaConsumer.notificationService.dao.NotificationRepository;
import com.kafkaConsumer.notificationService.dto.StatusMessage;
import com.kafkaConsumer.notificationService.entity.Notification;
import com.kafkaConsumer.notificationService.event.OrderEvent;
import com.kafkaConsumer.notificationService.util.EmailUtils;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender javaMailSender;
    private final EmailUtils emailUtils;
    @Value("${spring.mail.username}")
    private String orgEmail;

    @Transactional
    @Override
    public void sendNotification(OrderEvent orderEvent) {
        if (notificationRepository.existsByOrderId(orderEvent.id())) {
//            ensuring idempotence if order processed exists with id skip
            log.error("Notification already sent for order {}. Skipping...", orderEvent.id());
            return;
        } else {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
//                build mail details
                message.setSubject("Your Order is confirmed with order ID: ".concat(String.valueOf(orderEvent.id())));
                message.setFrom(orgEmail);
                message.setTo(orderEvent.customerEmail());
                message.setText(buildEmailBody(orderEvent));
//              send mail
                log.info("sending email notification to customer with masker email {} for Order ID {}", emailUtils.maskEmail(orderEvent.customerEmail()), orderEvent.id());
                javaMailSender.send(message);

//            build notification obj
                Notification notification
                        = Notification.builder()
                        .recipientEmail(orderEvent.customerEmail())
                        .orderId(orderEvent.id())
                        .sentAt(LocalDateTime.now())
                        .createdAt(orderEvent.paymentDateTime())
                        .statusMessage(StatusMessage.SUCCESS)
                        .build();
                notificationRepository.save(notification);

                log.info("Successfully sent notification for order {}", orderEvent.id());
            } catch (Exception e){
                log.error("Failed to send notification for order with ID {} with Exception {}", orderEvent.id(), e.getMessage());
                Notification notification
                        = Notification.builder()
                        .recipientEmail(orderEvent.customerEmail())
                        .orderId(orderEvent.id())
                        .sentAt(LocalDateTime.now())
                        .statusMessage(StatusMessage.FAILED)
                        .errorMessage(e.getMessage())
                        .createdAt(orderEvent.paymentDateTime())
                        .build();
                notificationRepository.save(notification);
                throw new RuntimeException("Failed to send notification for order with ID ".concat(String.valueOf(orderEvent.id())));
            }

        }
    }

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
