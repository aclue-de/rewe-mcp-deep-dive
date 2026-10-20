# Lab Guide

Your workbook for the three labs. Every part follows the same pattern:

- **Goal:** what this part is about, in one sentence.
- **Do:** the steps, with every command spelled out.
- **What do you see?** Questions to answer from what you observe. Write your answers down.
- **Hint:** collapsed. Open it only if you're stuck.

**Today at a glance:**

| Lab | Time | Start with | You will… |
|---|---|---|---|
| 1 | 30 min | `git switch lab-1` | speak MCP by hand, then watch Claude Code do the same |
| 2 | 45 min | `git switch lab-2` | fix a badly designed tool, then build a tool, a resource and a prompt |
| 3 | 45 min | `git switch lab-3` | attack your own server and defend it |

**One rule for all labs: the server terminal (T1) is the truth.** Claude's answer tells you what Claude *says* happened. T1 shows every request that really reached the server.

**Something doesn't work?** Check `README.md`, section 7 (Troubleshooting).

**Before Lab 1:** setup from `README.md`, section 1. Work in pairs: one person types, one reads along and asks questions. Switch roles after each lab.

**You always need two terminals, both in the project folder:**
- **T1:** the server (`java -jar target/mini-jira-mcp.jar`). You watch it.
- **T2:** Claude Code (`claude`) or `SendMessages`. You type here.

**The rebuild loop** (after every code change and after every `git switch`):

| # | Where | What |
|---|---|---|
| 1 | T1 | `Ctrl+C` (stop the server) |
| 2 | T1 | Build: PowerShell `.\mvnw.cmd clean package` · cmd `mvnw.cmd clean package` · Git Bash/macOS/Linux `./mvnw clean package` |
| 3 | T1 | `java -jar target/mini-jira-mcp.jar` |
| 4 | Claude Code | `/mcp` → `mini-jira` → **Reconnect** |

Build fails with "file in use" or "failed to delete"? The server is still running in T1. Stop it first (step 1).

**Commit before switching labs.** Git won't switch with uncommitted changes:

```
git commit -am "my lab 2 solution"
git switch lab-3
```

**Resetting data:** comments written during a lab only live in the server's memory. Restart the server and everything is back to the original.

**Claude Code knows the project:** at the start of every session it reads `CLAUDE.md`, which imports `AGENTS.md` (structure, commands, conventions, and rules such as "don't solve exercises on your own initiative"). See `README.md`, section 5.

---

## Lab 1 · How an MCP server works (30 min)

**Start:** `git switch lab-1`, then build (rebuild loop steps 2–3).

### Part A · Start the server

**Goal:** see what the server announces when it starts.

**Do:**
1. T1: `java -jar target/mini-jira-mcp.jar`

**What do you see?**
- On which address and port does the server listen?
- How many tools, resources and prompts does it register?
- Does anything happen in T1 while nobody is connected?

### Part B · You are the client (no Claude Code)

**Goal:** speak MCP yourself, message by message, the way Claude Code will in a minute.

`scripts/SendMessages.java` sends the lines of a `.jsonl` file to the running server, one JSON-RPC message per line, and prints every response.

**How to read the output** (shortened):

```
=== Message 1 ===
--> {"jsonrpc":"2.0","id":1,"method":"initialize", ...}
    (server opened session 984e82db-9b47-4e2d-b4bb-3486df217320)
<-- HTTP 200
{ "jsonrpc": "2.0", "id": 1, "result": { "protocolVersion": "2025-06-18", ... } }

=== Message 2 ===
--> {"jsonrpc":"2.0","method":"notifications/initialized"}
<-- 202 Accepted: a notification, so there is no response
```

- `-->` is the message you sent. `<--` is the server's answer.
- `(server opened session …)` means the server just gave you an ID. SendMessages sends it along with every later message.
- `202 Accepted` is not an error: the message was received, it just doesn't get an answer.

**Do:**
1. T2: `java scripts/SendMessages.java`
   Watch T1 at the same time.
2. Make your own copy of the message file:

   | Shell | Command |
   |---|---|
   | PowerShell | `Copy-Item scripts\handshake.jsonl scripts\my.jsonl` |
   | cmd | `copy scripts\handshake.jsonl scripts\my.jsonl` |
   | Git Bash, macOS, Linux | `cp scripts/handshake.jsonl scripts/my.jsonl` |

3. Open `scripts/my.jsonl` in your editor. Add a line that calls `get_issue` for ticket `SHOP-103`. Everything you need (method, tool name, parameter name) is in the responses from step 1. Run:
   `java scripts/SendMessages.java scripts/my.jsonl`
4. Add two more lines that go wrong on purpose:
   - `get_issue` for a ticket that doesn't exist (e.g. `SHOP-999`);
   - a method that doesn't exist (e.g. `"method":"foo/bar"`).

   Run the file again.
5. Look at the other two primitives: `java scripts/SendMessages.java scripts/primitives.jsonl`

**What do you see?**
- Which protocol version did the client ask for in `initialize`, and which one did the server answer with?
- After `initialize`, what does the line `(server opened session ...)` mean? Why does the client need it for every later message?
- Why does `notifications/initialized` get no response?
- What does the server tell you about each tool? Which values can `status` take in `search_issues`?
- Compare the two errors from step 4. What is different about them? **Which of the two would the model get to see?**
- What does a resource return? What does a prompt return? How is that different from a tool result?
- What appears in T1 for every line you send?

<details>
<summary>Hint: the line for get_issue</summary>

Copy the `tools/call` line from `handshake.jsonl` and change the tool name and the `arguments`. Give it a new `id`. The parameter names are in the `inputSchema` of the `tools/list` response.
</details>

### Part C · Claude Code is the client

**Goal:** see which messages Claude Code sends for you.

**How Claude Code finds the server:** the project folder contains a file `.mcp.json` with the server's address (`http://localhost:8080/mcp`). When you start `claude` in the project folder, Claude Code reads that file and connects automatically. Claude Code only connects to the server; it doesn't start it, so the server must be running in T1.

**Want to try it from another folder?** Started anywhere else, Claude Code doesn't know about `mini-jira`. Register it once in that folder (it only applies there):

```
claude mcp add --transport http mini-jira http://localhost:8080/mcp
```

Then start `claude` in that folder. To remove it again, run `claude mcp remove mini-jira` in the same folder.

**Do:**
1. T2: `claude` (approve the `mini-jira` server if asked). Watch T1 while it connects.
2. Check `/mcp`: is `mini-jira` connected? Look at **View tools**.
3. Ask, one at a time, and watch T1 after each question:
   - "Which tickets in Sprint 42 are blocked, and who are they assigned to?"
   - "What is Lena currently working on?"
   - "How is this MCP server built?"

   Remember what the last answer shows you. It comes back in Lab 3.

**What do you see?**
- Which messages does Claude Code send when it connects (T1)? Compare with your `handshake.jsonl`.
- Which tool does Claude pick for each question, with which arguments?
- Which JSON-RPC message from Part B did Claude Code just send for you?

### Part D · Does every question reach the server?

**Goal:** find out when the model actually calls the server, and when it doesn't.

**Do:**
1. Ask Claude the same question 3 to 5 times in a row, e.g. "What is Lena currently working on?" Watch T1 each time.
2. Type `/clear`, then ask the same question once more. Watch T1.

**What do you see?**
- Does every question produce a `Tool call` line in T1?
- What changes after `/clear`? Why?

**Done when:** you can explain (1) which of your two errors in Part B the model would actually see, and (2) why a question in Part D sometimes doesn't reach the server at all.

---

## Lab 2 · Building tools, resources and prompts (45 min)

**Start:** `git switch lab-2`, then the rebuild loop.

**Using Claude Code is allowed**, with three rules:

1. **Diagnose first, then let Claude type.** Work out yourself what's wrong or what's needed before you ask Claude to change code.
2. **You decide, Claude types.** Write tool names, descriptions and parameter descriptions **yourself**: they are what the model sees, and they are the point of this lab. Claude may write the logic.
3. **Don't let Claude look at the solution branches.** If you're stuck, run `git diff lab-2-solution -- src` yourself, so you decide how much you want to see.

**Tests are optional:** Parts B and C come with ready-made test classes, switched off with `@Disabled`. If you want feedback on your result, remove `@Disabled` and run the tests (PowerShell `.\mvnw.cmd test` · cmd `mvnw.cmd test` · Git Bash/macOS/Linux `./mvnw test`). Trying it out with Claude Code is what counts.

Every spot you have to work on is marked in the code with a comment like:

```java
// ============================================================
// Lab 2 · Part B: implement get_sprint_report here.
// ============================================================
```

### Syntax reference

You'll need these three patterns. The examples are deliberately neutral; they are not the solutions.

**A tool** (the model decides when to call it):

```java
@McpTool(
        name = "get_example",
        description = "What it does. When the model should use it, and when not.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true))   // only reads data
public String getExample(
        @McpToolParam(description = "Sprint name, e.g. 'Sprint 42'") String sprint) {
    if (/* the input makes no sense */) {
        // becomes a result with isError: true, which the model reads
        throw new ToolCallException("What went wrong, and what to do instead.");
    }
    return "Markdown text for the model";
}
```

**A resource** (the user or the app attaches it as context; the model doesn't call it):

```java
@McpResource(
        uri = "mini-jira://example",
        name = "example",
        title = "Example",
        description = "What this content is.",
        mimeType = "text/markdown")
public String example() {
    return "# Markdown content";
}
```

**A prompt** (the user starts it as a slash command, with arguments):

```java
@McpPrompt(name = "example-prompt", description = "What this workflow does.")
public PromptMessage examplePrompt(
        @McpArg(name = "key", description = "Ticket key, e.g. SHOP-101", required = true) String key) {
    return new PromptMessage(Role.USER, new TextContent("Instructions for the model about " + key));
}
```

Existing examples in the project: `IssueTools.java` (tools), `ConventionsResource.java` (resource), `SprintSummaryPrompt.java` (prompt). Each class is an ordinary Spring bean (`@Component`); data comes from `IssueStore` via the constructor.

### Part A · The colleague's tool (15 min)

**Goal:** feel how much a tool's design decides whether the model can use it.

A colleague added a tool to answer "Who in the team is overloaded?". It works, but something is off. It's in `src/main/java/de/aclue/training/minijira/mcp/TeamTools.java`.

**Do:**
1. Ask Claude Code: "Who has the most open work in Sprint 42? Is anyone overloaded?" Watch T1.

   Careful: Claude's answer may look correct. Check T1 to see which tool was called. If the colleague's tool wasn't used at all, that is your first indication that the tool has not been designed properly.
2. Read the tool's code. List everything that makes it hard for a model to use. Think about: name, description, parameters, what it returns, what happens on errors, and whether the host can tell that it only reads data.
3. Fix it. Rebuild loop. Ask the question from step 1 again.

**What do you see?**
- Before the fix: did Claude use the colleague's tool at all? If yes, how many times and with which arguments? If not, what did it use instead?
- After the fix: is the question answered with **one** call to your tool?
- In `/mcp` → **View tools**: how is your tool labeled now compared to before?

<details>
<summary>Hint</summary>

Look at the tool from the model's side: `/mcp` → View tools shows exactly what the model gets. Would *you* know what `t` and `f` mean? What does the model get back: something it can read easily, or a raw dump?
</details>

### Part B · Your own tool (15 min)

**Goal:** build your own tool from a skeleton.

`src/main/java/de/aclue/training/minijira/mcp/SprintTools.java` contains a skeleton for `get_sprint_report`. Optional self-check: `SprintToolsTest` and `SprintToolsDataConsistencyTest`.

**Do:**
1. Implement it. For a sprint name, return a short Markdown report with:
   - how many tickets are in each status;
   - the blocked tickets (key, summary, assignee);
   - the open (not done) high-priority tickets;
   - for an unknown sprint: a tool error that tells the caller which sprints exist.
2. Write the description and the parameter description. When should the model pick this tool instead of `search_issues`?
3. Rebuild loop. Ask Claude: "How is Sprint 42 doing?" and then "How is Sprint 99 doing?"

**What do you see?**
- Is "How is Sprint 42 doing?" answered with one call to `get_sprint_report`?
- What does Claude do with your error message for Sprint 99?

<details>
<summary>Hint</summary>

`store.search(null, null, sprint)` gives you all tickets of a sprint; `store.findSprintNames()` lists the existing sprints. The test classes show which texts the report is expected to contain.
</details>

### Part C · Your own resource and prompt (15 min)

**Goal:** build the two other primitives and use them from Claude Code.

**C1 · Resource `mini-jira://sprints`** in `SprintOverviewResource.java`

**Do:**
1. Implement a resource with the URI `mini-jira://sprints` that returns a Markdown table of all sprints with the number of tickets per status, in exactly this format:
   ```
   # Sprints

   | Sprint | Total | To do | In progress | Blocked | Done |
   |---|---|---|---|---|---|
   | Sprint 41 | 6 | 0 | 1 | 0 | 5 |
   ```
2. Optional self-check: `SprintOverviewResourceTest`.

**C2 · Prompt `ticket-triage`** in `TicketTriagePrompt.java`

**Do:**
1. Implement a prompt named `ticket-triage` with one required argument `key`. It returns an instruction for the model: load the ticket with `get_issue`, read the Definition of Done (resource `docs://team/conventions`), list what the ticket doesn't cover yet, and suggest next steps.
2. Optional self-check: `TicketTriagePromptTest`.

**Try both in Claude Code** (rebuild loop first):
1. Type `@mini` and pick `mini-jira://sprints` from the suggestions, then add: "Which sprint has the most open work?"
2. Type `/mcp__mini-jira__ticket-triage SHOP-104` and send it.

**What do you see?**
- T1 after step 1: what is logged, and when: when you attach the resource or when the model answers?
- T1 after step 2: what does the prompt request look like? Which tools does Claude call afterwards?
- What does the triage find for SHOP-104?
- Tool, resource, prompt: who decided each time that it gets used?

<details>
<summary>Hint</summary>

The resource needs `IssueStore` in its constructor (like `SprintTools`). For the counts per status: `store.search(null, null, sprint)` and count by `issue.status()`. The prompt needs no data at all: it only builds a text.
</details>

### Extra · One more tool (if you have time)

**Do:** build `get_stale_tickets` in `StaleTicketTools.java`: tickets that are in progress or blocked but haven't been updated for a number of days (each ticket has an `updated` date). Ask Claude: "Which tickets have been stuck for more than two weeks?"

**Done when:** "Who is overloaded in Sprint 42?" and "How is Sprint 42 doing?" are each answered with exactly one call to your tool, and the resource and the prompt work in Claude Code.

**Stuck?** `git diff lab-2-solution -- src` shows how the solution differs from your code. `git switch lab-2-solution` lets you try it (commit your own work first).

---

## Lab 3 · Attack, defend (45 min)

**Goal:** experience attacks that reach the model *through the server*, and stop them at more than one level: the model, the host (Claude Code), and the server code.

**Start:** `git switch lab-3`, then the rebuild loop. Check `/mcp` → View tools: the server now has a few more tools, including one that **writes** data (`add_comment`).

**Note:** Lab 3 starts from the sample solution of Lab 2, not from your own code. Your own Lab 2 work stays saved on the `lab-2` branch.

**Plan your time:** Parts A–D are the core. Part E is optional: do it if you have time left, or take it home.

**Keep an eye on T1** the whole time. It shows every tool call, so you can see what really happened, not just what Claude says happened.

### Part A · An attack through data

**Goal:** see that a tool result is just text for the model, including any instructions hidden in it.

**Do:**
1. Allow all mini-jira tools without asking. Create or edit `.claude/settings.local.json`:
   ```json
   { "permissions": { "allow": ["mcp__mini-jira"] } }
   ```
   (`mcp__mini-jira` without a tool name means *every* tool of that server.) Exit Claude Code (`/exit`) and start it again (`claude`).
2. Ask: "Summarize SHOP-117." Watch T1 closely.
3. Ask: "Summarize SHOP-124." This one is subtler.

**What do you see?**
- Did Claude call a tool you didn't ask for? Which one, with what arguments?
- Read the tickets' comments yourself (with `SendMessages` and `get_issue`, or ask Claude to show them verbatim). Who wrote them? What do they ask for?
- SHOP-117 and SHOP-124 want different things. What would each one cost in a real Jira?
- If Claude didn't fall for it: would you bet your production Jira on that, every time?

**If nothing happens:** that's a valid result too; today's models catch many of these. Write down what Claude said and continue: the next parts are about not having to rely on it.

**Afterwards:** restart the server (T1: `Ctrl+C`, start again) to remove any comments that were written.

### Part B · Defend with permission rules

**Goal:** let the host stop a tool call before it reaches the server, no matter what the model wants.

**Do:**
1. Replace the broad rule in `.claude/settings.local.json`: the reading tools on `allow`, every tool that writes on `ask`. Tool names have the form `mcp__mini-jira__<tool>`; copy them exactly from `/mcp`.
2. Restart Claude Code, repeat Part A, and **decline** when Claude Code asks.

**What do you see?**
- Which of the server's tools write data, and which only read? How can you tell from the tool list (Lab 1, Part B)?
- With `ask`, who is the last line of defense? What if that person approves without reading?

<details>
<summary>Hint: the shape of the settings file</summary>

The structure looks like this. Which tools go into which list is your decision; there is one entry per tool.

```json
{
  "permissions": {
    "allow": ["mcp__mini-jira__…", "mcp__mini-jira__…"],
    "ask":   ["mcp__mini-jira__…"]
  }
}
```
</details>

### Part C · Defend in the server

**Goal:** rules in Claude Code protect *your* setup. Rules in the server protect *everyone* who uses it.

**Do:**
1. In `IssueTools.java`, make the server harder to misuse:
   - mark comments in `get_issue`'s result clearly as unverified user content, not as instructions;
   - reject comments longer than 500 characters with a tool error;
   - allow at most 3 comments per session (each Claude Code session counts on its own).
2. Rebuild loop, then put the "allow everything" rule from Part A back into `.claude/settings.local.json`, restart Claude Code and repeat Part A. This time only the server can stop the attack.

**What do you see?**
- Which of the three rules makes the attack *impossible*, and which only make it *harder*?
- Would the label alone have been enough?
- Open a second Claude Code session: does it get its own 3 comments?

<details>
<summary>Hint: tool errors</summary>

A tool error is just an exception: look at how `get_issue` reports an unknown ticket.
</details>

<details>
<summary>Hint: "per session"</summary>

The server serves many clients at the same time, so a single counter would be shared by all of them. A tool can find out who is calling: add a parameter `McpSyncServerExchange exchange` to the tool method (it doesn't show up in the tool's schema), and `exchange.sessionId()` tells you the caller's session. Keep one counter per session, e.g. in a `Map<String, AtomicInteger>`.
</details>

### Part D · Something odd

**Goal:** see that a server can also attack you through its **tool definitions**, not only through data.

**Do:**
1. Restart Claude Code and ask a completely normal question, e.g. "Which tickets are blocked?"
2. Does anything about the answer look strange? If so, find out where it comes from. Claude Code won't tell you.

**What do you see?**
- Which tool is involved? Did Claude even call it (check T1)?
- How would you notice this in a server you didn't write, or after a harmless-looking update?
- What protects you: the model, the permission rules, or reviewing what the server sends?

**If nothing looks strange:** open Hint 2 anyway and read the tool list. The attack is there, whether or not Claude recognised it.

<details>
<summary>Hint 1</summary>

It isn't in the ticket data this time.
</details>

<details>
<summary>Hint 2</summary>

In Lab 1 you saw one response that contains everything the server tells the model about its tools. Look at it again: `java scripts/SendMessages.java`.
</details>

### Part E · Who may use which tool?

**Goal:** understand the difference between "the agent can't call it" and "nobody can call it".

**E1 · Forbid writing in Claude Code**

**Do:**
1. Add a `deny` rule for `mcp__mini-jira__add_comment` in `.claude/settings.local.json`, restart Claude Code, and ask Claude to add a comment to a ticket.
2. Now call it yourself, past Claude Code: `java scripts/SendMessages.java scripts/comment.jsonl`
3. Ask Claude Code: "Show me the comments of SHOP-105."

**What do you see?**
- What did Claude Code do in step 1? What does T1 show?
- What does T1 show for step 2? Was the comment written?
- Who enforces the `deny` rule: Claude Code or the server? What does that mean for any other program that talks to the same server?

**E2 · Forbid writing in the server**

The server already reads an HTTP header `X-Mini-Jira-Role` on every request (see `McpTransportConfig.java`). Without the header, the role is `writer`.

**Do:**
1. Remove the `deny` rule again.
2. Make `add_comment` refuse with a clear tool error when the role is `reader`.
3. Rebuild loop. Test as `reader` and as `writer` with `SendMessages`:
   ```
   java scripts/SendMessages.java scripts/comment.jsonl --header X-Mini-Jira-Role:reader
   java scripts/SendMessages.java scripts/comment.jsonl
   ```
4. Test it from Claude Code: add the header to `.mcp.json`, then restart Claude Code:
   ```json
   {
     "mcpServers": {
       "mini-jira": {
         "type": "http",
         "url": "http://localhost:8080/mcp",
         "headers": { "X-Mini-Jira-Role": "reader" }
       }
     }
   }
   ```
   Ask Claude to add a comment.

**What do you see?**
- How does Claude react to "permission denied"? Does it tell you, or try something else?
- Who sets the role in your setup, and who could change it? What would a real company server need instead?

<details>
<summary>Hint</summary>

Same parameter as in Part C: `McpSyncServerExchange exchange`. The role is in `exchange.transportContext().get(McpTransportConfig.ROLE)`.
</details>

**E3 · Hide a tool completely**

**Do:**
1. Think back to "How is this MCP server built?" (Lab 1). Should every user and every model be able to look into the server's internals?
2. Make `get_project_structure` exist only when a configuration flag is set (e.g. `mini-jira.admin-tools.enabled`, set via the environment variable `MINI_JIRA_ADMIN_TOOLS`).
3. Rebuild, then start the server once without and once with the flag, and check the tool count in T1 and in `java scripts/SendMessages.java`:

   | Shell | Start with the flag on |
   |---|---|
   | PowerShell | `$env:MINI_JIRA_ADMIN_TOOLS="true"; java -jar target/mini-jira-mcp.jar` |
   | cmd | `set MINI_JIRA_ADMIN_TOOLS=true` then `java -jar target/mini-jira-mcp.jar` (two lines) |
   | Git Bash, macOS, Linux | `MINI_JIRA_ADMIN_TOOLS=true java -jar target/mini-jira-mcp.jar` |

   To switch it off again in the same window: PowerShell `Remove-Item Env:MINI_JIRA_ADMIN_TOOLS` · cmd `set MINI_JIRA_ADMIN_TOOLS=` (Git Bash/macOS/Linux: just start without the prefix).

**What do you see?**
- What's the difference between a tool that refuses (E2) and a tool that doesn't exist (E3), for the model and for an attacker?
- Which of all the defenses today would still protect you if someone connected a different client to this server?

<details>
<summary>Hint</summary>

Spring can create a bean only when a property has a certain value: `@ConditionalOnProperty`. Look at how `MINI_JIRA_DATA` gets into the code via `application.properties`.
</details>

**Done when:** you can say which level stopped each attack in your setup (the model, the permission rules, or the server), and why relying on the model alone isn't enough.

**Stuck?** `git diff lab-3-solution -- src` shows how the hardened version differs from your code.

**Clean up when you're done:** delete `.claude/settings.local.json`. Git doesn't track this file, so a rule you set in Lab 3 (for example "allow every mini-jira tool" from Part A) would otherwise stay active.
