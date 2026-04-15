package com.workflow.agent.controller;

import com.workflow.agent.model.WorkflowRequest;
import com.workflow.agent.model.WorkflowResponse;
import com.workflow.agent.service.WorkflowAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowAgentService agentService;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/run")
    public WorkflowResponse run(@RequestBody WorkflowRequest request) {
        return agentService.run(request.getRequest());
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<WorkflowResponse> getRun(@PathVariable String runId) {
        return agentService.getRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
