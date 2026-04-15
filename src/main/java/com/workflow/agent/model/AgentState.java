package com.workflow.agent.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AgentState {
    private String request;
    @Builder.Default private AgentPhase phase = AgentPhase.PLAN;
    @Builder.Default private List<String> plan = new ArrayList<>();
    @Builder.Default private int currentStepIndex = 0;
    @Builder.Default private List<StepResult> stepResults = new ArrayList<>();
    @Builder.Default private List<ToolCall> toolCalls = new ArrayList<>();
    @Builder.Default private String finalAnswer = "";
    private String error;
}
