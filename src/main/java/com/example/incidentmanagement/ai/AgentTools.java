package com.example.incidentmanagement.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tool definitions exposed to the LLM via Spring AI tool calling.
 *
 * Each {@code @Tool}-annotated method below is discovered reflectively when this instance is
 * passed to a {@code ChatClient} request via {@code .tools(agentTools)}: Spring AI reads the
 * {@link Tool#description()} plus the method's single record parameter to build the JSON schema
 * advertised to the model, and invokes the matching method by name if the model chooses to call it.
 * (Superseded the older {@code @Bean Function<> + @Description} / {@code .functions("beanName")}
 * pattern, which was removed from {@code ChatClient} in Spring AI 1.1.)
 */
@Slf4j
@Component
public class AgentTools {

    public record TicketRequest(String title, String description) {
    }

    public record SlackRequest(String channel, String message) {
    }

    @Tool(description = """
            Creates a Jira engineering ticket to track remediation of a diagnosed production incident.
            Call this once you have identified a root cause and a concrete fix. The title should be a
            short one-line summary of the failure, and the description should contain the root-cause
            analysis and the recommended fix. Returns the created ticket ID (e.g. "ENG-101").""")
    public String createJiraTicketTool(TicketRequest request) {
        String ticketId = "ENG-" + ThreadLocalRandom.current().nextInt(100, 999);
        log.info("[JIRA] Created ticket {} | title='{}' | description='{}'",
                ticketId, request.title(), request.description());
        return ticketId;
    }

    @Tool(description = """
            Sends a Slack alert to notify the on-call engineering team about a diagnosed incident.
            Call this to immediately notify humans of the diagnosis, summarizing the issue and
            including the Jira ticket ID if one has already been created. Returns a confirmation string.""")
    public String sendSlackAlertTool(SlackRequest request) {
        log.info("[SLACK] #{} <- {}", request.channel(), request.message());
        return "Slack alert sent to #" + request.channel();
    }
}
