---
description: Save current session state to ~/.claude/sessions/ with all context, what worked, what failed, and exact next steps.
---

# Save Session Command

Write a timestamped session file to `~/.claude/sessions/` capturing all work context.

**File Location & Naming:**
- Directory: `~/.claude/sessions/`
- Format: `YYYY-MM-DD-<short-id>-session.tmp`
- Short-id rules: lowercase letters, digits, hyphens only; minimum 8 characters

**Mandatory Sections** (all must be included, even if empty):
1. What We Are Building
2. What WORKED (with evidence)
3. What Did NOT Work (and why — exact errors, not vague descriptions)
4. What Has NOT Been Tried Yet
5. Current State of Files (status table: path | status | notes)
6. Decisions Made
7. Blockers & Open Questions
8. Exact Next Step
9. Environment & Setup Notes (optional if not relevant)

**Critical Principles:**
- The "What Did NOT Work" section is the most critical — list every failed approach with the EXACT reason/error
- "An incomplete file is worse than an honest empty section" — write "Nothing yet" or "N/A" rather than omitting sections
- Gather context first, create the file, populate all sections honestly, then display for user confirmation before closing

**Process:**
1. `mkdir -p ~/.claude/sessions/`
2. Generate a short-id (8+ lowercase alphanumeric chars)
3. Write file at `~/.claude/sessions/YYYY-MM-DD-<short-id>-session.tmp`
4. Display full file contents for user confirmation
