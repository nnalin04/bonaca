---
description: Load the most recent session file from ~/.claude/sessions/ and resume work with full context from where the last session ended.
---

# Resume Session Command

Load the last saved session state and orient fully before doing any work.

## Usage

```
/resume-session                                                       # loads most recent file
/resume-session 2024-01-15                                            # loads most recent for that date
/resume-session ~/.claude/sessions/2024-01-15-abc123de-session.tmp   # loads specific file
```

## Process

### Step 1: Find the session file

If no argument: check `~/.claude/sessions/`, pick the most recently modified `*-session.tmp` file.

If none found:
```
No session files found in ~/.claude/sessions/
Run /save-session at the end of a session to create one.
```

### Step 2: Read the entire session file

Read the complete file. Do not summarize yet.

### Step 3: Confirm understanding

Respond with this exact format:

```
SESSION LOADED: [resolved path]
════════════════════════════════════════════════

PROJECT: [project name / topic from file]

WHAT WE'RE BUILDING:
[2-3 sentence summary in your own words]

CURRENT STATE:
✅ Working: [count] items confirmed
🔄 In Progress: [list files that are in progress]
🗒️ Not Started: [list planned but untouched]

WHAT NOT TO RETRY:
[list every failed approach with its reason — this is critical]

OPEN QUESTIONS / BLOCKERS:
[list any blockers or unanswered questions]

NEXT STEP:
[exact next step if defined]

════════════════════════════════════════════════
Ready to continue. What would you like to do?
```

### Step 4: Wait for the user

Do NOT start working automatically. Do NOT touch any files.

## Edge Cases

- **Session > 7 days old**: note "⚠️ This session is from N days ago. Things may have changed."
- **Referenced files missing**: note "⚠️ `path/to/file` referenced in session but not found on disk."
- **Empty/malformed file**: "Session file found but appears empty or unreadable."
- Never modify the session file when loading it — it is read-only historical record
