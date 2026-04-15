package com.workflow.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.agent.config.AnthropicClient;
import com.workflow.agent.model.*;
import com.workflow.agent.tools.AgentTool;
import com.workflow.agent.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the Plan → Route → Execute → Reflect state machine.
 *
 * This is the Java equivalent of a LangGraph compiled graph.
 * Each "node" is a private method that transforms AgentState.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowAgentService {

    private final AnthropicClient llm;
    private final ToolRegistry tools;
    private final ObjectMapper mapper = new ObjectMapper();

    // In-memory run store (swap for Redis / DB in production)
    private final Map<String, WorkflowResponse> runs = new LinkedHashMap<>();

    /**
     * Execute the full workflow for a plain-English request.
     */
    public WorkflowResponse run(String request) {
        var state = AgentState.builder().request(request).build();

        int maxIterations = 20; // safety guard
        int i = 0;
        while (state.getPhase() != AgentPhase.DONE && i++ < maxIterations) {
            state = switch (state.getPhase()) {
                case PLAN    -> plan(state);
                case ROUTE   -> route(state);
                case EXECUTE -> execute(state);
                case REFLECT -> reflect(state);
                default      -> state;
            };
        }

        var runId = UUID.randomUUID().toString().substring(0, 12);
        var response = WorkflowResponse.builder()
                .runId(runId)
                .status("completed")
                .steps(state.getStepResults())
                .finalAnswer(state.getFinalAnswer())
                .build();
        runs.put(runId, response);
        return response;
    }

    public Optional<WorkflowResponse> getRun(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    // ── Nodes ──────────────────────────────────────────────────────────────

    private AgentState plan(AgentState state) {
        log.info("PLAN: decomposing request");
        String prompt = """
            You are a workflow planner. Given the user request and available tools,
            produce a JSON array of step strings. Each step should be a short action
            sentence that can be fulfilled by one tool call.

            Available tools:
            %s

            User request: %s

            Respond ONLY with a JSON array of strings, no markdown.
            """.formatted(tools.describeAll(), state.getRequest());

        log.info("PLAN: prompt: " + prompt);
        String raw = llm.chat(prompt);
        List<String> plan;
        try {
            plan = mapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            plan = List.of(state.getRequest());
        }

        String result = plan.stream().collect(Collectors.joining(", "));
        log.info("PLAN: result: " + result);
        state.setPlan(plan);
        state.setPhase(AgentPhase.ROUTE);
        return state;
    }

    private AgentState route(AgentState state) {
        int idx = state.getCurrentStepIndex();
        if (idx >= state.getPlan().size()) {
            state.setPhase(AgentPhase.DONE);
            return state;
        }

        String step = state.getPlan().get(idx);
        log.info("ROUTE: step {} → {}", idx, step);

        String schemasJson;
        try { schemasJson = mapper.writeValueAsString(tools.toolSchemas()); }
        catch (Exception e) { schemasJson = "[]"; }

        String priorJson;
        try { priorJson = mapper.writeValueAsString(state.getStepResults()); }
        catch (Exception e) { priorJson = "[]"; }

        String prompt = """
            You are a tool router. Given a step description and tool schemas,
            return a JSON object with "tool" (tool name) and "args" (dict of arguments).

            Tool schemas:
            %s

            Step: %s

            Context from prior steps:
            %s

            Respond ONLY with a JSON object, no markdown.
            """.formatted(schemasJson, step, priorJson);

        log.info("ROUTE: prompt: " + prompt);
        String raw = llm.chat(prompt);
        ToolCall call;
        try {
            call = mapper.readValue(raw, ToolCall.class);
        } catch (Exception e) {
            call = ToolCall.builder().tool("db_query").args(Map.of("sql", "SELECT 1")).build();
        }

        log.info("ROUTE: tool selected: " + call.getTool());
        state.getToolCalls().add(call);
        state.setPhase(AgentPhase.EXECUTE);
        return state;
    }

    private AgentState execute(AgentState state) {
        ToolCall call = state.getToolCalls().get(state.getToolCalls().size() - 1);
        log.info("EXECUTE: {} with {}", call.getTool(), call.getArgs());

        String result;
        boolean success;
        try {
            AgentTool tool = tools.get(call.getTool());
            if (tool == null) throw new IllegalArgumentException("Unknown tool: " + call.getTool());
            result = tool.execute(call.getArgs() != null ? call.getArgs() : Map.of());
            String callArgs = call.getArgs().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ", "{", "}"));
            System.out.println(result);
            log.info("EXECUTE: tool call: " + tool.name() + ":" + callArgs);
            success = true;
        } catch (Exception e) {
            result = "Error: " + e.getMessage();
            success = false;
        }

        String argSummary;
        try { argSummary = mapper.writeValueAsString(call.getArgs()); }
        catch (Exception e) { argSummary = "{}"; }
        if (argSummary.length() > 200) argSummary = argSummary.substring(0, 200);

        state.getStepResults().add(StepResult.builder()
                .tool(call.getTool())
                .inputSummary(argSummary)
                .outputSummary(result.length() > 500 ? result.substring(0, 500) : result)
                .success(success)
                .build());
        state.setPhase(AgentPhase.REFLECT);
        return state;
    }

    private AgentState reflect(AgentState state) {
        int nextIdx = state.getCurrentStepIndex() + 1;
        if (nextIdx >= state.getPlan().size()) {
            log.info("REFLECT: all steps done, generating summary");
            String stepsJson;
            try { stepsJson = mapper.writeValueAsString(state.getStepResults()); }
            catch (Exception e) { stepsJson = "[]"; }

            String prompt = """
                You executed a multi-step workflow. Summarize the results for the user.

                Original request: %s
                Steps completed:
                %s

                Write a concise, friendly summary.
                """.formatted(state.getRequest(), stepsJson);

            log.info("REFLECT: prompt: " + prompt);
            String finalAnswer = llm.chat(prompt);
            state.setFinalAnswer(finalAnswer);
            log.info("Final answer: " + finalAnswer);
            state.setCurrentStepIndex(nextIdx);
            state.setPhase(AgentPhase.DONE);
        } else {
            log.info("REFLECT: moving to step {}", nextIdx);
            state.setCurrentStepIndex(nextIdx);
            state.setPhase(AgentPhase.ROUTE);
        }
        return state;
    }
}
