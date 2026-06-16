# Harness Audit Command

Run a deterministic Claude Code configuration audit and return a prioritized scorecard.

## Usage

`/harness-audit [scope]`

- `scope` (optional): `repo` (default), `hooks`, `skills`, `commands`, `agents`

## What It Audits

Evaluate 7 categories (0-10 each, 70 total for full repo scope):

1. **Tool Coverage** — Are all major task types covered by skills/commands?
2. **Context Efficiency** — Do hooks manage context (PreCompact, SessionStart)?
3. **Quality Gates** — Are post-edit type-check/format hooks configured?
4. **Memory Persistence** — Is there a working session save/resume system?
5. **Eval Coverage** — Are there `/learn`, `/skill-stocktake` flows?
6. **Security Guardrails** — Does pre-tool-use hook block destructive ops?
7. **Cost Efficiency** — Is there model routing + cost tracking?

## Output Format

```
Harness Audit (<scope>): XX/70
─────────────────────────────────
Tool Coverage:      X/10 — [finding]
Context Efficiency: X/10 — [finding]
Quality Gates:      X/10 — [finding]
Memory Persistence: X/10 — [finding]
Eval Coverage:      X/10 — [finding]
Security Guardrails:X/10 — [finding]
Cost Efficiency:    X/10 — [finding]

Top 3 Actions:
1) [Category] Specific actionable fix
2) [Category] Specific actionable fix
3) [Category] Specific actionable fix

Suggested skills to apply: /skill1, /skill2
```

## Process

1. Read `.claude/settings.json` and `.claude/hooks/`
2. Glob `.claude/commands/`, `.claude/skills/`, `.claude/agents/`
3. Check for `~/.claude/sessions/` directory existence
4. Score each category based on findings
5. Report top 3 highest-ROI actions

## Arguments

- `repo|hooks|skills|commands|agents` (optional scope)
