package com.workflow.agent.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicClientTest {

    private MockWebServer server;
    private AnthropicClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new AnthropicClient("test-key", server.url("/").toString());
        ReflectionTestUtils.setField(client, "model", "claude-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void chat_extractsTextFromFirstContentBlock() {
        server.enqueue(new MockResponse()
                .setBody("{\"content\":[{\"type\":\"text\",\"text\":\"Hello from Claude\"}]}")
                .addHeader("Content-Type", "application/json"));

        assertThat(client.chat("Say hello")).isEqualTo("Hello from Claude");
    }

    @Test
    void chat_returnsRawResponseWhenContentArrayIsEmpty() {
        String body = "{\"content\":[]}";
        server.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        assertThat(client.chat("test")).isEqualTo(body);
    }

    @Test
    void chat_returnsRawResponseWhenContentNodeMissing() {
        String body = "{\"model\":\"claude-test\",\"stop_reason\":\"end_turn\"}";
        server.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        assertThat(client.chat("test")).isEqualTo(body);
    }

    @Test
    void chat_returnsErrorMessageOnMalformedJson() {
        server.enqueue(new MockResponse()
                .setBody("not valid json at all")
                .addHeader("Content-Type", "application/json"));

        assertThat(client.chat("test")).startsWith("Error parsing response:");
    }
}
