package com.example.incidentmanagement.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.example.incidentmanagement.model.Severity;

import lombok.extern.slf4j.Slf4j;

/**
 * Sole point of contact with Gemini for incident analysis. This class knows nothing about
 * persistence, the Incident entity, or HTTP -- it only turns (title, description, severity)
 * into a structured {@link GeminiAnalysisResult}, or throws {@link GeminiAnalysisException}
 * once retries are exhausted. Keeping it this narrow is what lets the rest of the app treat
 * "ask the AI" as a single, swappable, independently-testable operation.
 */
@Slf4j
@Service
public class GeminiService {

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private static final String SYSTEM_PROMPT = """
            You are an expert Site Reliability Engineer assistant. Analyze the incident described
            by the user and produce a concise, actionable triage assessment.

            priorityScore must be an integer from 0 (no urgency) to 100 (drop everything, critical
            outage), reflecting business impact together with the reported severity -- not severity
            alone.
            """;

    // Retries transient failures (network blips, momentarily malformed JSON) up to 3 times
    // with exponential backoff before giving up. This is Gemini-call-specific retry, distinct
    // from -- and layered on top of -- Spring AI's own internal HTTP retry.
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public GeminiAnalysisResult analyze(String title, String description, Severity severity) {
        String userPrompt = """
                Title: %s
                Description: %s
                Severity: %s
                """.formatted(title, description, severity);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .entity(GeminiAnalysisResult.class);
    }

    // Invoked automatically once all @Retryable attempts have failed. Normalizes every
    // failure mode (API error, timeout, unparseable response) into one exception type so
    // callers only ever need to handle GeminiAnalysisException.
    @Recover
    public GeminiAnalysisResult recover(
            Exception ex,
            String title,
            String description,
            Severity severity) {

        log.error(
            "GEMINI FAILURE for incident '{}'. Type: {}. Message: {}",
            title,
            ex.getClass().getName(),
            ex.getMessage(),
            ex
        );

        throw new GeminiAnalysisException(
            "Gemini analysis failed for incident: " + title,
            ex
        );
    }
}
