package com.workflow.agent.tools;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GmailSearchTool implements AgentTool {

    @Override public String name() { return "gmail_search"; }

    @Override public String description() {
        return "Search Gmail for emails matching a query. Returns subject + snippet JSON.";
    }

    @Override public Map<String, Map<String, String>> parameters() {
        return Map.of(
            "query", Map.of("type", "string", "description", "Gmail search query"),
            "max_results", Map.of("type", "integer", "description", "Max emails to return")
        );
    }

    @Override public String execute(Map<String, Object> args) {
        // TODO: wire up Gmail API
        return """
            [{"subject":"Ticket #1042 — Login broken","snippet":"User cannot login since update...","date":"2025-04-01"},
             {"subject":"Ticket #1051 — Billing issue","snippet":"Double charged on March invoice...","date":"2025-04-03"}]
            """;
    }
}
