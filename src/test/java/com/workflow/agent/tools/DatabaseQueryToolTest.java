package com.workflow.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseQueryToolTest {

    private DatabaseQueryTool tool;

    @BeforeEach
    void setUp() { tool = new DatabaseQueryTool(); }

    @Test
    void name_isDbQuery() {
        assertThat(tool.name()).isEqualTo("db_query");
    }

    @Test
    void parameters_containsSql() {
        assertThat(tool.parameters()).containsKey("sql");
    }

    @Test
    void execute_returnsJsonArray() {
        String result = tool.execute(Map.of("sql", "SELECT * FROM tickets"));
        assertThat(result).startsWith("[").endsWith("]");
    }
}
