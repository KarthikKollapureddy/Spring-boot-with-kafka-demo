package com.kafkaConsumer.notificationService.util;

import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        String maskedLocal = localPart.length() > 2
                ? localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1)
                : "***";

        return maskedLocal + "@" + domain;
    }

}
