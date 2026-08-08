package com.example.incidentmanagement.ai;

/**
 * Thrown when Gemini analysis could not be completed after retries were exhausted --
 * either the call itself failed or the response couldn't be parsed into a
 * {@link GeminiAnalysisResult}. Caught by {@link IncidentAnalysisService}, never allowed
 * to propagate to a controller.
 */
public class GeminiAnalysisException extends RuntimeException {

    public GeminiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
