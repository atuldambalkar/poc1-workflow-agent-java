package com.workflow.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry(List.of(
                new SlackPostTool(),
                new GmailSearchTool(),
                new GmailSendTool(),
                new DatabaseQueryTool()
        ));
    }

    @Test
    void get_returnsToolByName() {
        assertThat(registry.get("slack_post")).isInstanceOf(SlackPostTool.class);
    }

    @Test
    void get_returnsNullForUnknownTool() {
        assertThat(registry.get("nonexistent_tool")).isNull();
    }

    @Test
    void all_containsAllRegisteredTools() {
        assertThat(registry.all()).hasSize(4);
    }

    @Test
    void describeAll_containsEachToolName() {
        String description = registry.describeAll();
        assertThat(description)
                .contains("slack_post")
                .contains("gmail_search")
                .contains("gmail_send")
                .contains("db_query");
    }

    @Test
    void toolSchemas_returnsOneSchemaPerTool() {
        assertThat(registry.toolSchemas()).hasSize(4);
    }

    @Test
    void toolSchemas_containsNameAndDescription() {
        registry.toolSchemas().forEach(schema -> {
            assertThat(schema).containsKey("name");
            assertThat(schema).containsKey("description");
        });
    }
}
