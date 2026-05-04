package com.workflow.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.agent.model.WorkflowResponse;
import com.workflow.agent.service.WorkflowAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WorkflowAgentService agentService;

    @Test
    void health_returns200WithStatusOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void run_returns200WithWorkflowResponse() throws Exception {
        var response = WorkflowResponse.builder()
                .runId("abc123456789")
                .status("completed")
                .steps(List.of())
                .finalAnswer("All done.")
                .build();
        when(agentService.run(anyString())).thenReturn(response);

        mockMvc.perform(post("/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\": \"Post a Slack digest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("abc123456789"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.finalAnswer").value("All done."));
    }

    @Test
    void getRun_returns200ForExistingRunId() throws Exception {
        var response = WorkflowResponse.builder()
                .runId("run-001")
                .status("completed")
                .steps(List.of())
                .finalAnswer("Done.")
                .build();
        when(agentService.getRun("run-001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/runs/run-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-001"));
    }

    @Test
    void getRun_returns404ForUnknownRunId() throws Exception {
        when(agentService.getRun("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/runs/unknown"))
                .andExpect(status().isNotFound());
    }
}
