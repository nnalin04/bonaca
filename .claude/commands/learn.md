# /learn - Extract Reusable Patterns

Analyze the current session and extract patterns worth saving as skills.

## What to Extract

1. **Error Resolution Patterns** — root cause + what fixed it + is it reusable?
2. **Debugging Techniques** — non-obvious steps, tool combinations that worked
3. **Workarounds** — library quirks, API limitations, version-specific fixes
4. **Project-Specific Patterns** — codebase conventions, architecture decisions, integration patterns

## Output Format

Create a skill file at `~/.claude/skills/learned/[pattern-name].md`:

```markdown
# [Descriptive Pattern Name]

**Extracted:** [Date]
**Context:** [Brief description of when this applies]

## Problem
[What problem this solves - be specific]

## Solution
[The pattern/technique/workaround]

## Example
[Code example if applicable]

## When to Use
[Trigger conditions]
```

## Process

1. Review the session for extractable patterns
2. Identify the most valuable/reusable insight
3. Draft the skill file
4. Ask user to confirm before saving
5. Save to `~/.claude/skills/learned/`

## Notes

- Don't extract trivial fixes (typos, simple syntax errors)
- Don't extract one-time issues (specific API outages)
- Focus on patterns that save time in future sessions
- One pattern per skill file
