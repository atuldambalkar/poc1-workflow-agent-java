package com.workflow.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GmailSendToolTest {

    private GmailSendTool tool;

    @BeforeEach
    void setUp() { tool = new GmailSendTool(); }

    @Test
    void name_isGmailSend() {
        assertThat(tool.name()).isEqualTo("gmail_send");
    }

    @Test
    void parameters_containsToSubjectBody() {
        assertThat(tool.parameters()).containsKeys("to", "subject", "body");
    }

    @Test
    void execute_returnsConfirmationWithRecipient() {
        String result = tool.execute(Map.of("to", "test@example.com", "subject", "Hi", "body", "Hello"));
        assertThat(result).containsIgnoringCase("test@example.com");
    }
}
