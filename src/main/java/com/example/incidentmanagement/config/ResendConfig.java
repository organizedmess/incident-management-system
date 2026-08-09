package com.example.incidentmanagement.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Resend HTTPS API client used by NotificationService. Resend replaces the
 * previous JavaMailSender/Gmail SMTP setup, which Render's free tier blocks (outbound
 * SMTP on port 587 times out there).
 */
@Configuration
public class ResendConfig {

    @Bean
    public Resend resend(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
