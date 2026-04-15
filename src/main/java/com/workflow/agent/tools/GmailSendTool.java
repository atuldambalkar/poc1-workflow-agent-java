package com.workflow.agent.tools;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GmailSendTool implements AgentTool {

    @Override public String name() { return "gmail_send"; }

    @Override public String description() { return "Send an email via Gmail."; }

    @Override public Map<String, Map<String, String>> parameters() {
        return Map.of(
            "to", Map.of("type", "string", "description", "Recipient email"),
            "subject", Map.of("type", "string", "description", "Email subject"),
            "body", Map.of("type", "string", "description", "Email body")
        );
    }

    @Override public String execute(Map<String, Object> args) {
        // TODO: wire up Gmail API
        return "Email sent to " + args.get("to") + " with subject '" + args.get("subject") + "'";
    }
}
