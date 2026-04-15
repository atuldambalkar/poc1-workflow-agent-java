package com.workflow.agent.tools;

import java.util.Map;

/**
 * Contract for all agent tools.
 */
public interface AgentTool {
    /** Unique tool name used by the LLM to select it. */
    String name();

    /** Human-readable description for the LLM. */
    String description();

    /** JSON-schema-style parameter descriptions. */
    Map<String, Map<String, String>> parameters();

    /** Execute the tool with the given arguments. */
    String execute(Map<String, Object> args);
}
