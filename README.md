# Mini-Jira MCP Server

Exercise project for the MCP Deep Dive: a small Java / Spring AI **MCP server over HTTP** that offers a fictitious ticket tracker ("Mini-Jira") to Claude Code as **tools, resources and prompts**. You start the server yourself in one terminal and connect Claude Code from a second one. Every request shows up live in the server terminal.

```
Terminal 1: the server                       Terminal 2: the client
┌──────────────────────────┐                 ┌──────────────────────────────┐
│ java -jar ...            │ ◄── HTTP ────── │ claude                        │
│ http://localhost:8080/mcp│     POST /mcp   │ (or java scripts/SendMessages)│
│ logs every message       │                 │                              │
└──────────────────────────┘                 └──────────────────────────────┘
```

No accounts, no real data. The server only listens on your own machine (`127.0.0.1`).

The tasks for each lab are in **[project_docs/LABS_PLAYBOOK.md](project_docs/LABS_PLAYBOOK.md)**. This README is the reference: setup, how to start, how it works, troubleshooting.

**Which terminal am I in?** The commands below are given for every shell. Look at your prompt:

| Prompt looks like | Shell |
|---|---|
| `PS C:\...>` | PowerShell (Windows) |
| `C:\...>` | cmd (Windows) |
| `user@host ... $` | Git Bash (Windows), macOS or Linux terminal |

Commands that start with `java` work the same in every shell.

---

## 1. Setup (once, about 5 minutes)

**You need:** JDK 21 (`java` on your PATH, check with `java -version`), Git, and the Claude Code CLI (`claude`). Maven is not needed: the project brings its own Maven Wrapper.

**Step 1: clone and open the project folder** (the one with `pom.xml`). All commands in this README run from there.

**Step 2: build.**

| Shell | Command |
|---|---|
| PowerShell | `.\mvnw.cmd clean package` |
| cmd | `mvnw.cmd clean package` |
| Git Bash, macOS, Linux | `./mvnw clean package` |
| any shell, if Maven is installed | `mvn clean package` |

✔ **What you should see:** `BUILD SUCCESS` at the end, and the file `target/mini-jira-mcp.jar` exists.

> macOS/Linux: if you get `permission denied: ./mvnw`, run `chmod +x mvnw` once.

---

## 2. Start the server and connect (every lab)

**Step 1: start the server in terminal 1.**

```
java -jar target/mini-jira-mcp.jar
```

✔ **What you should see** (after about 5 seconds):

```
... Registered tools: 3
... Registered resources: 1
... Registered prompts: 1
... Tomcat started on port 8080 (http) with context path '/'
... Started MiniJiraApplication in 4.3 seconds
```

The numbers depend on the lab. Leave this terminal open: every message the server receives appears here. **Stop the server only with `Ctrl+C` in this window.** If you just close the window, the server may keep running in the background and block the port.

**Step 2: connect Claude Code from terminal 2** (also in the project folder).

```
claude
```

When Claude Code asks whether it may use the project server `mini-jira` (from `.mcp.json`), approve it.

✔ **What you should see:**
- in **terminal 1**: `Client initialize request ...`, then `Received JSON message:` lines with `tools/list`, `resources/list`, `prompts/list`;
- in **Claude Code**: `/mcp` lists `mini-jira` as connected.

> If `/mcp` shows `mini-jira` as *pending* or *disabled*: select it in `/mcp` and approve or enable it.

**Step 3: after every code change (the rebuild loop).**

1. Terminal 1: `Ctrl+C` (stop the server).
2. Build (command from section 1).
3. Terminal 1: `java -jar target/mini-jira-mcp.jar` (start again).
4. Claude Code: `/mcp` → `mini-jira` → **Reconnect**.

> **Always stop the server before you build.** On Windows a running server locks `target/mini-jira-mcp.jar`, and the build fails with "file is being used by another process".

**Optional settings when starting the server** (environment variables):

| Variable | Default | What it does |
|---|---|---|
| `MINI_JIRA_PORT` | `8080` | Port of the server. If you change it, change the URL in `.mcp.json` too |
| `MINI_JIRA_ADMIN_TOOLS` | `false` | `true` switches on the admin tool `get_project_structure` (final version) |
| `MINI_JIRA_DATA` | `data/issues.json` | Ticket file |

How to set one, e.g. `MINI_JIRA_PORT=8081`:

| Shell | Command |
|---|---|
| PowerShell | `$env:MINI_JIRA_PORT="8081"; java -jar target/mini-jira-mcp.jar` |
| cmd | `set MINI_JIRA_PORT=8081` then `java -jar target/mini-jira-mcp.jar` (two lines) |
| Git Bash, macOS, Linux | `MINI_JIRA_PORT=8081 java -jar target/mini-jira-mcp.jar` |

In PowerShell and cmd the variable stays set for that window until you close it. Remove it with `Remove-Item Env:MINI_JIRA_PORT` (PowerShell) or `set MINI_JIRA_PORT=` (cmd).

---

## 3. How the server works

### One endpoint, three primitives

The server has exactly **one** HTTP endpoint: `POST http://localhost:8080/mcp`. It is not a REST API. Every request is a JSON-RPC message that says what the client wants, e.g. `{"method": "tools/call", "params": {"name": "search_issues", ...}}`.

Behind that endpoint the server offers three kinds of things, the MCP **primitives**:

| Primitive | What it is | Who decides to use it | In Claude Code |
|---|---|---|---|
| **Tool** | A function the model can call (`search_issues`) | **The model** | Claude calls it on its own |
| **Resource** | Content the client can read (`docs://team/conventions`) | **The user / the app** | Attach it with `@`, e.g. `@mini-jira:docs://team/conventions` |
| **Prompt** | A prepared instruction with arguments (`sprint-summary`) | **The user** | Start it as a slash command: `/mcp__mini-jira__sprint-summary` |

### What happens on a request

```
"What is Lena working on?"
  → the model picks a tool:              search_issues(assignee = "lena")
  → Claude Code sends an HTTP POST:      {"method":"tools/call","params":{"name":"search_issues",...}}
  → Spring AI calls the Java method:     IssueTools.searchIssues(null, "lena", null)
  → the method returns text; Spring AI wraps it into a JSON-RPC result
  → Claude Code hands the text to the model, which writes the answer
```

MCP over HTTP is **stateful**: on `initialize` the server opens a session and returns an `Mcp-Session-Id` header. The client sends it along with every later message. Each Claude Code session (and each run of `SendMessages`) gets its own session.

### Project structure

```
├── AGENTS.md                       Instructions for AI assistants (see section 5)
├── CLAUDE.md                       Claude Code's entry point, imports AGENTS.md
├── .mcp.json                       How Claude Code finds the server (URL)
├── .claude/settings.json           Shared Claude Code settings for everyone on the project
├── pom.xml                         Spring Boot + Spring AI MCP server (WebMVC)
├── data/
│   ├── issues.json                 The tickets (fictitious)
│   └── team-calendar.json          Vacations and on-call duty (from Lab 3)
├── docs/definition-of-done.md      Served as the resource docs://team/conventions
├── scripts/
│   ├── SendMessages.java           Plays the client: sends a .jsonl file, prints the answers
│   ├── handshake.jsonl             Default messages: handshake, tools/list, one tool call
│   ├── primitives.jsonl            Lists and reads resources and prompts
│   └── comment.jsonl               Calls add_comment (from Lab 3)
├── logs/mini-jira.log              Copy of the server log (created on first start)
└── src/main/
    ├── resources/
    │   ├── application.properties  Server name, port, HTTP transport, data paths
    │   └── logback-spring.xml      Logging: terminal + file
    └── java/de/aclue/training/minijira/
        ├── MiniJiraApplication.java  Spring Boot entry point
        ├── domain/                   Plain records and enums: Issue, Comment, Status, ...
        ├── store/IssueStore.java     Loads issues.json at startup, searches it
        └── mcp/                      Everything a client can see:
            ├── IssueTools.java         @McpTool methods (search_issues, get_issue, ...)
            ├── ConventionsResource.java  @McpResource
            ├── SprintSummaryPrompt.java  @McpPrompt
            ├── McpTransportConfig.java   Passes the X-Mini-Jira-Role header to the tools (Lab 3)
            └── ToolCallException.java  Thrown by a tool → result with isError: true
```

**The important idea:** tools, resources and prompts are **ordinary Spring beans**. A method annotated with `@McpTool`, `@McpResource` or `@McpPrompt` becomes visible to clients. Names and descriptions are all the model ever learns about them. You can unit-test them like any other Java class, without MCP.

### Where the data comes from

- **Tickets:** `data/issues.json`, about 20 fictitious tickets in three sprints (`Sprint 41`, `Sprint 42`, `Sprint 43`).
- **The server loads the file once at startup and keeps everything in memory.** Changes (e.g. a comment written by a tool) live only until the server stops. **Restarting the server resets the data**, and the file itself is never changed.
- **Team calendar:** `data/team-calendar.json` (from Lab 3).
- **Definition of Done:** `docs/definition-of-done.md`.
- All names and systems are invented. Nothing leaves your machine.

---

## 4. Labs

Each lab has its own Git branch. Labs 2 and 3 also have a solution branch.

| Lab | What you do | Start | Solution |
|---|---|---|---|
| **1 · How an MCP server works** | Talk to the server by hand with raw JSON-RPC, connect Claude Code, and watch the server terminal while you ask questions. | `git switch lab-1` | (no code) |
| **2 · Building tools, resources and prompts** | Fix a colleague's badly designed tool, build your own tool, resource and prompt. | `git switch lab-2` | `git switch lab-2-solution` |
| **3 · Attack, defend** | Watch attacks that arrive through tool results and tool descriptions, and stop them with permission rules and server code. | `git switch lab-3` | `git switch lab-3-solution` |

`main` is the same as `lab-3-solution`.

**After every switch:** stop the server, build, start it again (rebuild loop, section 2).

**Commit your work before switching labs.** Git won't switch while you have uncommitted changes. Your commit stays on your local copy of the lab branch, so you can always come back:

```
git commit -am "my lab 2 solution"
git switch lab-3
```

These git commands are the same in every shell.

---

## 5. Working with Claude Code in this repository

This repository is also an example of how to set up a project for AI-assisted development. These files tell Claude Code (and other assistants) how to work here:

| File | In Git? | Purpose |
|---|---|---|
| `AGENTS.md` | yes | **Instructions for any AI assistant:** what the project is, the build commands, the structure, the coding conventions, and rules (e.g. "don't solve exercises on your own"). Read by Claude Code, Copilot, Cursor, Codex and others |
| `CLAUDE.md` | yes | Claude Code reads this file automatically at the start of every session. Here it only imports `AGENTS.md` (`@AGENTS.md`), so there is one set of instructions for every tool |
| `.mcp.json` | yes | The MCP servers this project uses. Everyone who clones the repo gets the same servers |
| `.claude/settings.json` | yes | Claude Code settings shared by the whole team, e.g. permission rules |
| `.claude/settings.local.json` | **no** (gitignored) | Your personal settings and permission rules. You'll use it in Lab 3 |

**Good practice for your own projects:**
- Keep `AGENTS.md` short and concrete: commands that work, where things are, conventions, what not to touch. Skip anything the assistant can read from the code itself.
- Write rules as instructions ("throw `ToolCallException` for errors"), not as background stories.
- Check it into Git and update it when the structure or the conventions change, like any other code.
- Personal preferences go into local files (`.claude/settings.local.json`), team rules into the shared ones.

To see what Claude Code loaded, type `/memory` in Claude Code.

## 6. Tools for looking inside

| What | How |
|---|---|
| **Watch the server live** | Terminal 1. Every incoming message appears as `Received JSON message: ...`, every tool call as `Tool call: ...` |
| **Play the client by hand** | `java scripts/SendMessages.java` (default: `scripts/handshake.jsonl`), or `java scripts/SendMessages.java scripts/my.jsonl` for your own file. One JSON-RPC message per line; keep the first two lines of `handshake.jsonl` at the top. The server must be running |
| **Send an HTTP header** | `java scripts/SendMessages.java scripts/comment.jsonl --header X-Mini-Jira-Role:reader` |
| **Server status in Claude Code** | `/mcp` → `mini-jira`: connection status, list of tools (**View tools**), **Reconnect** |
| **Health check** | send `{"jsonrpc":"2.0","id":9,"method":"ping"}`; the answer is an empty result |
| **Permission rules** | `.claude/settings.local.json`, tool names as `mcp__mini-jira__<tool>` (copy them from `/mcp`) |

**With curl** (optional, Git Bash/macOS/Linux; PowerShell 5 garbles the JSON quotes):

```bash
curl -i http://localhost:8080/mcp \
  -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'
```

The response headers contain the `Mcp-Session-Id`; send it as `-H "Mcp-Session-Id: <id>"` with every further request.

---

## 7. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `mvnw.cmd` is not recognized (PowerShell) | PowerShell needs `.\` | `.\mvnw.cmd clean package` |
| `./mvnw: Permission denied` (macOS/Linux) | Execute bit missing | `chmod +x mvnw` |
| Build fails: "being used by another process" | The server is still running | `Ctrl+C` in terminal 1, then build |
| `Port 8080 was already in use` | An old server still runs (e.g. its window was closed) | Find and stop it, see below. Or use another port (`MINI_JIRA_PORT`, section 2) |
| `No server at localhost:8080` (SendMessages) | Server not started, or a different port | Start it in terminal 1; set the same `MINI_JIRA_PORT` in both terminals |
| `mini-jira` missing in `/mcp` | Claude Code started outside the project folder | Exit, `cd` into the project folder, `claude` |
| `mini-jira` *pending* / *disabled* in `/mcp` | Not approved yet | `/mcp` → `mini-jira` → approve/enable |
| `mini-jira` *failed* in `/mcp` | Server not running | Start it, then `/mcp` → Reconnect |
| New or changed tool not visible | Not rebuilt, server not restarted, or not reconnected | Rebuild loop (section 2, step 3) |
| Permission rule has no effect | Tool name typo, or Claude Code not restarted | Copy the exact name from `/mcp`, restart Claude Code |

**Find and stop the process on port 8080:**

| Shell | Find | Stop |
|---|---|---|
| PowerShell | `Get-NetTCPConnection -LocalPort 8080 -State Listen` (column `OwningProcess`) | `Stop-Process -Id <pid>` |
| cmd | `netstat -ano \| findstr :8080` (last column) | `taskkill /F /PID <pid>` |
| Git Bash | `netstat -ano \| grep :8080` (last column) | `taskkill //F //PID <pid>` |
| macOS, Linux | `lsof -i :8080` (column `PID`) | `kill <pid>` |
