package com.workflow.agent.service;

import com.workflow.agent.config.AnthropicClient;
import com.workflow.agent.tools.AgentTool;
import com.workflow.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowAgentServiceTest {

    @Mock AnthropicClient llm;
    @Mock ToolRegistry tools;
    @InjectMocks WorkflowAgentService service;

    @Test
    void run_returnCompletedResponseForSingleStepWorkflow() {
        AgentTool mockTool = mock(AgentTool.class);
        when(mockTool.execute(any())).thenReturn("Posted successfully.");
        when(tools.describeAll()).thenReturn("slack_post: Post to Slack");
        when(tools.toolSchemas()).thenReturn(List.of());
        when(tools.get("slack_post")).thenReturn(mockTool);

        when(llm.chat(anyString()))
                .thenReturn("[\"Post a message to Slack\"]")
                .thenReturn("{\"tool\":\"slack_post\",\"args\":{\"channel\":\"#general\",\"message\":\"hello\"}}")
                .thenReturn("Workflow complete. Message posted to Slack.");

        var response = service.run("Post hello to Slack #general");

        assertThat(response).isNotNull();
        assertThat(response.getRunId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getSteps()).hasSize(1);
        assertThat(response.getSteps().get(0).getTool()).isEqualTo("slack_post");
        assertThat(response.getSteps().get(0).isSuccess()).isTrue();
        assertThat(response.getFinalAnswer()).isNotBlank();
    }

    @Test
    void run_handlesInvalidPlanJsonWithFallback() {
        AgentTool mockTool = mock(AgentTool.class);
        when(mockTool.execute(any())).thenReturn("ok");
        when(tools.describeAll()).thenReturn("db_query: Query DB");
        when(tools.toolSchemas()).thenReturn(List.of());
        when(tools.get("db_query")).thenReturn(mockTool);

        // plan returns non-JSON → falls back to List.of(request)
        // route returns valid tool call
        when(llm.chat(anyString()))
                .thenReturn("not valid json")
                .thenReturn("{\"tool\":\"db_query\",\"args\":{\"sql\":\"SELECT 1\"}}")
                .thenReturn("Done.");

        var response = service.run("Run a DB query");

        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getSteps()).hasSize(1);
    }

    @Test
    void run_recordsFailureWhenToolNotFound() {
        when(tools.describeAll()).thenReturn("slack_post: Post to Slack");
        when(tools.toolSchemas()).thenReturn(List.of());
        when(tools.get("unknown_tool")).thenReturn(null);

        when(llm.chat(anyString()))
                .thenReturn("[\"Do something\"]")
                .thenReturn("{\"tool\":\"unknown_tool\",\"args\":{}}")
                .thenReturn("Completed with errors.");

        var response = service.run("Do something");

        assertThat(response.getSteps()).hasSize(1);
        assertThat(response.getSteps().get(0).isSuccess()).isFalse();
    }

    @Test
    void getRun_returnsEmptyForUnknownRunId() {
        assertThat(service.getRun("does-not-exist")).isEmpty();
    }

    @Test
    void getRun_returnsResponseAfterRun() {
        AgentTool mockTool = mock(AgentTool.class);
        when(mockTool.execute(any())).thenReturn("done");
        when(tools.describeAll()).thenReturn("slack_post: Post to Slack");
        when(tools.toolSchemas()).thenReturn(List.of());
        when(tools.get("slack_post")).thenReturn(mockTool);
        when(llm.chat(anyString()))
                .thenReturn("[\"Post to Slack\"]")
                .thenReturn("{\"tool\":\"slack_post\",\"args\":{\"channel\":\"#x\",\"message\":\"y\"}}")
                .thenReturn("All done.");

        var response = service.run("Post to Slack");
        var retrieved = service.getRun(response.getRunId());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRunId()).isEqualTo(response.getRunId());
    }

    @Test
    void run_executesMultiStepWorkflow() {
        AgentTool mockTool = mock(AgentTool.class);
        when(mockTool.execute(any())).thenReturn("result");
        when(tools.describeAll()).thenReturn("db_query: Query DB");
        when(tools.toolSchemas()).thenReturn(List.of());
        when(tools.get("db_query")).thenReturn(mockTool);

        when(llm.chat(anyString()))
                .thenReturn("[\"Query DB for data\", \"Post summary to Slack\"]") // plan: 2 steps
                .thenReturn("{\"tool\":\"db_query\",\"args\":{\"sql\":\"SELECT 1\"}}")  // route step 1
                // reflect step 1 (not last) — no LLM call, just moves to next step
                .thenReturn("{\"tool\":\"db_query\",\"args\":{\"sql\":\"SELECT 2\"}}")  // route step 2
                .thenReturn("Both steps completed.");                                   // reflect step 2 (final)

        var response = service.run("Query DB then summarize");

        assertThat(response.getSteps()).hasSize(2);
        assertThat(response.getStatus()).isEqualTo("completed");
    }
}
