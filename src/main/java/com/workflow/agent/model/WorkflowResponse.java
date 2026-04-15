package com.workflow.agent.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowResponse {
    private String runId;
    private String status;
    @Builder.Default private List<StepResult> steps = new ArrayList<>();
    @Builder.Default private String finalAnswer = "";
}
