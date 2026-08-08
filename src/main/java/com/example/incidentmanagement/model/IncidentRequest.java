package com.example.incidentmanagement.model;

public record IncidentRequest(String serviceName, String errorMessage, String stackTrace) {
}
