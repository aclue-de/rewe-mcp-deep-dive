# AGENTS.md

Instructions for AI coding assistants (Claude Code, GitHub Copilot, Cursor, Codex, ...) working in this repository. Humans: start with `README.md`.

## Project in one minute

Mini-Jira is a small **MCP server over HTTP** (Java 21, Spring Boot 3.5, Spring AI 1.1) built for a hands-on training. It offers a fictitious ticket tracker to MCP clients through one endpoint, `http://localhost:8080/mcp`, as **tools**, **resources** and **prompts**. There is no REST API and no database: tickets are loaded from `data/issues.json` at startup and kept in memory.

The user runs the server in its own terminal and talks to it from Claude Code (as an MCP client) or with `scripts/SendMessages.java`.

## Commands

Run everything from the project root.

| Task | Git Bash / macOS / Linux | PowerShell | cmd |
|---|---|---|---|
| Build the jar | `./mvnw clean package` | `.\mvnw.cmd clean package` | `mvnw.cmd clean package` |
| Unit tests | `./mvnw test` | `.\mvnw.cmd test` | `mvnw.cmd test` |
| All tests incl. integration test | `./mvnw clean verify` | `.\mvnw.cmd clean verify` | `mvnw.cmd clean verify` |
| Start the server | `java -jar target/mini-jira-mcp.jar` | same | same |
| Send JSON-RPC by hand (server must run) | `java scripts/SendMessages.java [file.jsonl] [--header Name:Value]` | same | same |

**After every code change** the user has to: stop the server (`Ctrl+C`), build, start it again, and reconnect in Claude Code (`/mcp` → `mini-jira` → Reconnect). On Windows a running server locks the jar, so the build fails while it runs. Remind the user of this loop instead of trying to restart the server yourself.

## Project structure

```
src/main/java/de/aclue/training/minijira/
├── MiniJiraApplication.java    Spring Boot entry point
├── domain/                     Records and enums (Issue, Comment, Status, Priority, ...)
├── store/IssueStore.java       Loads data/issues.json, searches it, keeps changes in memory
└── mcp/                        Everything MCP clients see: tools, resources, prompts
src/main/resources/             application.properties (port, transport), logback-spring.xml
src/test/java/...               Unit tests per class; integration/McpServerIT talks MCP over HTTP
data/, docs/                    Ticket data and the Definition of Done (lab material)
scripts/                        SendMessages.java and example .jsonl message files
project_docs/LABS_PLAYBOOK.md   The lab tasks
```

## How code is written here

- **Tools, resources and prompts are plain Spring beans** (`@Component`) in the `mcp` package. A method becomes visible to clients through `@McpTool`, `@McpResource` or `@McpPrompt`. The annotations live in `org.springaicommunity.mcp.annotation` (not `org.springframework.ai...`).
- **Data access** goes through `IssueStore`, injected via the constructor.
- **Tool results are text for a model:** short Markdown, not raw JSON dumps.
- **Errors:** throw `ToolCallException` with a message that tells the model what to do next. The framework turns it into a result with `isError: true`, which the model can read. Don't return error strings as normal results.
- **Read-only tools** declare `annotations = @McpTool.McpAnnotations(readOnlyHint = true)`. Unset hints default to "may modify data".
- **Logging:** one line per call through SLF4J, e.g. `log.info("Tool call: get_issue(key={})", key);`. Never `System.out`.
- **Tests:** plain unit tests that call the bean's method directly (JUnit 5, AssertJ, Mockito). No Spring context needed.
- **Dependencies:** don't add any. The pinned versions in `pom.xml` stay as they are.
- **Language:** English for all names, descriptions, messages and comments.

## This is a training repository

Every lab has its own branch. The code grows from branch to branch:

| Branch | State |
|---|---|
| `lab-1` | Basic server: a few tools, one resource, one prompt |
| `lab-2` | Lab 2 exercises: a badly designed tool to fix, skeletons to implement |
| `lab-2-solution` | Lab 2 solved |
| `lab-3` | Lab 3 material: more tools and data |
| `lab-3-solution` / `main` | Everything solved |

So on a lab branch, **missing or unfinished code is usually intentional**:
- A class or tool that doesn't exist yet may arrive with a later lab branch. Don't create it unless the user asks.
- Spots marked with a comment like `// Lab 2 · Part B: ...`, methods returning `"TODO"`, and tests marked `@Disabled` are **exercises** for the user.

## Rules for AI assistants

1. **Don't solve exercises on your own initiative.** Implement an exercise spot only when the user asks for it. If you notice one while doing something else, mention it and leave it.
2. **Names and descriptions are the user's job.** Tool names, descriptions and parameter descriptions are the learning goal of Lab 2. When they are missing, ask the user for them, or clearly mark yours as a suggestion for the user to rewrite.
3. **Don't look at the solution branches** (`lab-2-solution`, `lab-3-solution`, `main`, e.g. via `git show` or `git diff`) unless the user explicitly asks you to.
4. **Leave lab material alone** unless asked: `data/`, `docs/`, `scripts/*.jsonl`, and the existing tool descriptions are part of the exercises.
5. **Explain what you changed** and which step of the rebuild loop the user has to do next.

## More information

- `README.md`: setup, starting the server, how it works, troubleshooting.
- `project_docs/LABS_PLAYBOOK.md`: the tasks of every lab.
