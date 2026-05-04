# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Requires Java 21 and Maven 3.9+ installed globally (no Maven wrapper present).

```bash
# Build
mvn clean package

# Run (requires ANTHROPIC_API_KEY set)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Package without tests
mvn clean package -DskipTests
```

Set required environment variables before running:
```
ANTHROPIC_API_KEY=sk-ant-...          # required
```

Optional config in `src/main/resources/application.properties`:
- `slack.bot-token` — enables Slack posting
- `gmail.credentials-path` — enables Gmail tools

Server starts on port 8080.

## Architecture

This is a Spring Boot 3.3 / Java 21 application implementing an **LLM-driven agent loop** using the Anthropic API directly (no SDK — custom `AnthropicClient`).

### Agent State Machine

The core loop in `WorkflowAgentService` cycles through these phases:

```
PLAN → ROUTE → EXECUTE → REFLECT → (loop back to ROUTE or) DONE
```

- **PLAN**: LLM decomposes the user request into a list of steps
- **ROUTE**: LLM selects which tool to call and with what arguments for the current step
- **EXECUTE**: The selected tool is invoked via `ToolRegistry`
- **REFLECT**: LLM summarizes the step result and decides whether to proceed to the next step or finish

`AgentState` is the data object threaded through all phases — it holds the original request, current phase, plan, per-step results, and final answer.

### Tool System

Tools live in `src/main/java/com/workflow/agent/tools/` and implement `AgentTool`:

```java
public interface AgentTool {
    String getName();
    String getDescription();
    Map<String, Object> getParameters(); // JSON schema for LLM
    String execute(Map<String, Object> arguments);
}
```

Tools are Spring `@Component` beans. `ToolRegistry` auto-discovers them via constructor injection and dynamically generates JSON schemas for the LLM. To add a new tool: implement `AgentTool`, annotate with `@Component` — it is automatically available to the agent.

Current tools (`GmailSearchTool`, `GmailSendTool`, `SlackPostTool`, `DatabaseQueryTool`) are **stubs** — they return mock data.

### Key Classes

| Class | Role |
|---|---|
| `WorkflowAgentService` | State machine orchestrator |
| `AnthropicClient` | HTTP wrapper for Anthropic Messages API (uses `WebClient.block()`) |
| `ToolRegistry` | Discovers tools; generates LLM-facing schemas |
| `AgentState` | Mutable state threaded through agent phases |
| `WorkflowController` | REST: `POST /run`, `GET /runs/{runId}`, `GET /health` |

### Run Storage

Completed runs are stored in an in-memory `LinkedHashMap<String, WorkflowResponse>` inside `WorkflowController` — not persisted across restarts.

### LLM Model

Configured via `anthropic.model` in `application.properties`, defaulting to `claude-sonnet-4-20250514`. Update this to use newer models.