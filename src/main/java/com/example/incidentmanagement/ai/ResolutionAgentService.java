package com.example.incidentmanagement.ai;

import com.example.incidentmanagement.model.IncidentLog;
import com.example.incidentmanagement.repository.IncidentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG + function-calling agent that triages a stored incident.
 *
 * The RAG loop is: embed the incident's error signature -> retrieve the most similar past
 * incidents from pgvector -> inject their content into the system prompt as grounding context ->
 * let the LLM reason over both the new failure and that history -> let the LLM invoke tools
 * (Jira/Slack) to act on its own diagnosis.
 */
@Slf4j
@Service
public class ResolutionAgentService {

    private static final int TOP_K_SIMILAR_INCIDENTS = 3;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final IncidentLogRepository incidentLogRepository;
    private final AgentTools agentTools;

    // ChatClient.Builder is registered as a prototype bean by Spring AI's autoconfiguration
    // specifically so each consumer builds (and can customize) its own immutable ChatClient
    // instance instead of sharing one mutable client across the app.
    public ResolutionAgentService(ChatClient.Builder chatClientBuilder,
                                   VectorStore vectorStore,
                                   IncidentLogRepository incidentLogRepository,
                                   AgentTools agentTools) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.incidentLogRepository = incidentLogRepository;
        this.agentTools = agentTools;
    }

    public String diagnoseAndResolve(Long incidentId) {
        IncidentLog incident = incidentLogRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found: " + incidentId));

        // --- RETRIEVAL (the "R" in RAG) -----------------------------------------------
        // The current incident's error message + stack trace is embedded and used as the
        // similarity-search query against pgvector. This surfaces past incidents whose
        // embeddings are semantically closest to this one -- i.e. failures with a similar
        // shape -- even when the exact wording differs. topK caps how much history we pull
        // in, keeping the retrieved context small and directly relevant to this failure.
        String querySignature = incident.getErrorMessage() + "\n" + incident.getStackTrace();

        List<Document> similarIncidents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(querySignature)
                        .topK(TOP_K_SIMILAR_INCIDENTS)
                        .build());

        log.info("Retrieved {} similar past incident(s) for incident {}", similarIncidents.size(), incidentId);

        // --- AUGMENTATION (the "A" in RAG) ----------------------------------------------
        // The retrieved documents are flattened into plain text and embedded directly into the
        // system prompt below. This is the actual "context injection" step: the LLM never
        // queries the vector store itself, it only ever sees the text we hand it here, so the
        // model's diagnosis is grounded in real historical incidents instead of being inferred
        // from the stack trace alone.
        String historicalContext = similarIncidents.isEmpty()
                ? "No similar past incidents were found in the knowledge base."
                : similarIncidents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

        String systemPrompt = """
                You are an expert Site Reliability Engineer (SRE) agent responsible for triaging production incidents.

                You will be given details of a NEW incident and a set of HISTORICAL INCIDENTS retrieved because
                they are semantically similar to the new one. Use the historical incidents as grounding context
                to deduce the root cause and a concrete fix for the new incident -- prefer a fix consistent with
                that history over speculation.

                Once you have a diagnosis and a fix, you MUST:
                1. Call the `createJiraTicketTool` function to open an engineering ticket. The title should be a
                   concise one-line summary of the failure; the description should contain your root-cause
                   analysis and recommended fix.
                2. Call the `sendSlackAlertTool` function targeting the "#incidents" channel, summarizing the
                   diagnosis and including the Jira ticket ID returned by the previous call.

                Call both tools before giving your final answer. Your final text response should summarize the
                diagnosis and the fix, and confirm that the ticket and alert were created.

                HISTORICAL INCIDENTS (retrieved via vector similarity search):
                %s
                """.formatted(historicalContext);

        String userPrompt = """
                Diagnose the following incident:

                Service: %s
                Error Message: %s
                Stack Trace:
                %s
                """.formatted(incident.getServiceName(), incident.getErrorMessage(), incident.getStackTrace());

        // --- GENERATION with tool calling (the "G" in RAG) --------------------------
        // `.tools(agentTools)` reflectively scans the AgentTools instance for @Tool-annotated
        // methods, advertises their JSON schema to the model alongside the prompt, and -- if the
        // model chooses to call one -- transparently invokes the matching method and feeds its
        // return value back to the model before it produces the final text response returned here.
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(agentTools)
                .call()
                .content();
    }
}
