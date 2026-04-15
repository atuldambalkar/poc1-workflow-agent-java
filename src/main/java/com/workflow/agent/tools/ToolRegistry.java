package com.workflow.agent.tools;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Collects all AgentTool beans and provides lookup + schema generation.
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool> toolBeans) {
        toolBeans.forEach(t -> tools.put(t.name(), t));
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }

    /** Build a description block the LLM can use to pick tools. */
    public String describeAll() {
        var sb = new StringBuilder();
        for (var t : tools.values()) {
            sb.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
            t.parameters().forEach((k, v) ->
                sb.append("    ").append(k).append(" (").append(v.get("type")).append("): ").append(v.get("description")).append("\n")
            );
        }
        return sb.toString();
    }

    /** JSON tool schemas for Anthropic tool-use format. */
    public List<Map<String, Object>> toolSchemas() {
        var schemas = new ArrayList<Map<String, Object>>();
        for (var t : tools.values()) {
            var props = new LinkedHashMap<String, Object>();
            t.parameters().forEach((k, v) -> props.put(k, Map.of("type", v.getOrDefault("type", "string"), "description", v.getOrDefault("description", ""))));
            schemas.add(Map.of(
                "name", t.name(),
                "description", t.description(),
                "input_schema", Map.of("type", "object", "properties", props, "required", new ArrayList<>(t.parameters().keySet()))
            ));
        }
        return schemas;
    }
}
