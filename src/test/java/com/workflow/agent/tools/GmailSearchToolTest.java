package com.workflow.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GmailSearchToolTest {

    private GmailSearchTool tool;

    @BeforeEach
    void setUp() { tool = new GmailSearchTool(); }

    @Test
    void name_isGmailSearch() {
        assertThat(tool.name()).isEqualTo("gmail_search");
    }

    @Test
    void parameters_containsQueryAndMaxResults() {
        assertThat(tool.parameters()).containsKeys("query", "max_results");
    }

    @Test
    void execute_returnsJsonArray() {
        String result = tool.execute(Map.of("query", "support", "max_results", "3"));
        assertThat(result).startsWith("[").endsWith("]\n");
    }
}
