package com.example.incidentmanagement.ai;

/**
 * Target shape for Gemini's structured JSON response, bound via Spring AI's
 * {@code ChatClient.CallResponseSpec.entity(Class)} (backed by {@code BeanOutputConverter}),
 * which auto-generates the JSON schema/format instructions injected into the prompt and
 * parses the model's response back into this record.
 */
public record GeminiAnalysisResult(
        String summary,
        String rootCause,
        String recommendedAction,
        Integer priorityScore
) {
}
