package com.example.incidentmanagement.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code notification.email.recipients} from application.yml (or its env var
 * override). A single comma-separated value binds to a one-element list automatically
 * via Spring Boot's relaxed binding -- no special-casing needed for the single-recipient case.
 */
@ConfigurationProperties(prefix = "notification.email")
public record NotificationProperties(List<String> recipients) {
}
