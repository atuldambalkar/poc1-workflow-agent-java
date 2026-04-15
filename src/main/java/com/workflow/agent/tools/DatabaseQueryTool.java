package com.workflow.agent.tools;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class DatabaseQueryTool implements AgentTool {

    @Override public String name() { return "db_query"; }
    @Override public String description() { return "Run a read-only SQL query and return results as JSON."; }

    @Override public Map<String, Map<String, String>> parameters() {
        return Map.of("sql", Map.of("type", "string", "description", "SQL SELECT query"));
    }

    @Override public String execute(Map<String, Object> args) {
        // TODO: wire up JDBC / JPA
        return "[{\"id\":1,\"title\":\"Sample row\",\"status\":\"open\"}]";
    }
}
