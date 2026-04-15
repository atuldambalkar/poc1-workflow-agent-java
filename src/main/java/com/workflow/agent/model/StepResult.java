package com.workflow.agent.model;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StepResult {
    private String tool;
    private String inputSummary;
    private String outputSummary;
    private boolean success;
}
