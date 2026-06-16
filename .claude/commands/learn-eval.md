# /learn-eval - Extract, Evaluate, then Save

Extends `/learn` with a quality gate, save-location decision, and knowledge-placement check before writing any skill file.

## Process

1. Review the session for extractable patterns
2. Identify the most valuable/reusable insight

3. **Determine save location:**
   - Ask: "Would this pattern be useful in a different project?"
   - **Global** (`~/.claude/skills/learned/`): Generic patterns usable across 2+ projects
   - **Project** (`.claude/skills/learned/` in current project): Project-specific knowledge
   - When in doubt, choose Global

4. Draft the skill file:

```markdown
---
name: pattern-name
description: "Under 130 characters"
user-invocable: false
origin: auto-extracted
---

# [Descriptive Pattern Name]

**Extracted:** [Date]
**Context:** [Brief description of when this applies]

## Problem
[What problem this solves]

## Solution
[The pattern/technique/workaround - with code examples]

## When to Use
[Trigger conditions]
```

5. **Quality gate — Checklist + Holistic verdict**

   ### Checklist (verify by actually reading files)
   - [ ] Grep `~/.claude/skills/` for content overlap
   - [ ] Check MEMORY.md for overlap
   - [ ] Consider whether appending to an existing skill would suffice
   - [ ] Confirm this is a reusable pattern, not a one-off fix

   ### Holistic verdict

   | Verdict | Meaning | Next Action |
   |---------|---------|-------------|
   | **Save** | Unique, specific, well-scoped | Proceed to save |
   | **Improve then Save** | Valuable but needs refinement | List improvements → revise → re-evaluate |
   | **Absorb into [X]** | Should be appended to an existing skill | Show target + additions → append |
   | **Drop** | Trivial, redundant, or too abstract | Explain reasoning and stop |

6. Present checklist results + verdict rationale + full draft → save after user confirmation

## Output Format for Step 5

```
### Checklist
- [x] skills/ grep: no overlap (or: overlap found → details)
- [x] MEMORY.md: no overlap (or: overlap found → details)
- [x] Existing skill append: new file appropriate (or: should append to [X])
- [x] Reusability: confirmed (or: one-off → Drop)

### Verdict: Save / Improve then Save / Absorb into [X] / Drop

**Rationale:** (1-2 sentences)
```

## Notes

- Don't extract trivial fixes or one-time issues
- One pattern per skill file
- When the verdict is Absorb, append to the existing skill rather than creating a new file
