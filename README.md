# PoC 1 — Workflow Automation Agent (Java / Spring Boot)

AI agent that takes plain-English business requests and executes them using
tools (Gmail, Slack, Database). Built with a **state-machine agent loop**
served via **Spring Boot**.

## Architecture

```
User Request (plain English)
        │
   POST /run
        │
   WorkflowAgentService (state machine)
   ┌────┴────┐
   │  Plan   │  ← LLM decomposes request into steps
   └────┬────┘
        │
   ┌────▼────┐
   │  Route  │  ← Pick tool + args for current step
   └────┬────┘
        │
   ┌────▼─────────────────┐
   │  Execute Tool        │
   │  • GmailSearchTool   │
   │  • GmailSendTool     │
   │  • SlackPostTool     │
   │  • DatabaseQueryTool │
   └────┬─────────────────┘
        │
   ┌────▼────┐
   │ Reflect │  ← Loop or summarize
   └────┬────┘
        │
   Final response
```

## Quick Start

```bash
# 1. Set your API key
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Build & run
./mvnw spring-boot:run

# 3. Test
curl -X POST http://localhost:8080/run \
  -H "Content-Type: application/json" \
  -d '{"request": "Summarize support tickets from this week and post a Slack digest"}'
```

## Project Structure

```
src/main/java/com/poc1/agent/
├── WorkflowAgentApplication.java   # Entry point
├── config/
│   └── AnthropicClient.java        # Anthropic API wrapper
├── controller/
│   └── WorkflowController.java     # REST endpoints
├── model/
│   ├── AgentPhase.java             # State machine phases
│   ├── AgentState.java             # State threaded through nodes
│   ├── StepResult.java             # Per-step result
│   ├── ToolCall.java               # Tool invocation record
│   ├── WorkflowRequest.java        # API request DTO
│   └── WorkflowResponse.java       # API response DTO
├── service/
│   └── WorkflowAgentService.java   # Core state-machine agent
└── tools/
    ├── AgentTool.java              # Tool interface
    ├── ToolRegistry.java           # Auto-discovers all tools
    ├── GmailSearchTool.java        # Stub — wire up Gmail API
    ├── GmailSendTool.java          # Stub
    ├── SlackPostTool.java          # Stub — wire up Slack SDK
    └── DatabaseQueryTool.java      # Stub — wire up JDBC
```

## Adding a New Tool

1. Create a class implementing `AgentTool`
2. Annotate with `@Component`
3. It auto-registers via `ToolRegistry`

## Requirements

- Java 21+
- Maven 3.9+
