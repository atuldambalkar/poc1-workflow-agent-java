package com.workflow.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SlackPostToolTest {

    private SlackPostTool tool;

    @BeforeEach
    void setUp() { tool = new SlackPostTool(); }

    @Test
    void name_isSlackPost() {
        assertThat(tool.name()).isEqualTo("slack_post");
    }

    @Test
    void description_isNotBlank() {
        assertThat(tool.description()).isNotBlank();
    }

    @Test
    void parameters_containsRequiredKeys() {
        assertThat(tool.parameters()).containsKeys("channel", "message");
    }

    @Test
    void execute_returnsConfirmationWithChannel() {
        String result = tool.execute(Map.of("channel", "#ops", "message", "hello"));
        assertThat(result).contains("#ops");
    }
}
