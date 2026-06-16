# Checkpoint Command

Create or verify a git-integrated workflow checkpoint.

## Usage

`/checkpoint [create|verify|list] [name]`

## Create Checkpoint

1. Run `/verify quick` to ensure current state is clean
2. Create a git stash or commit with checkpoint name
3. Log checkpoint to `.claude/checkpoints.log`:

```bash
echo "$(date +%Y-%m-%d-%H:%M) | $CHECKPOINT_NAME | $(git rev-parse --short HEAD)" >> .claude/checkpoints.log
```

4. Report checkpoint created

## Verify Checkpoint

Compare current state to checkpoint:
- Files added/modified since checkpoint
- Test pass rate now vs then
- Coverage now vs then

Output:
```
CHECKPOINT COMPARISON: $NAME
============================
Files changed: X
Tests: +Y passed / -Z failed
Coverage: +X% / -Y%
Build: [PASS/FAIL]
```

## List Checkpoints

Show all checkpoints with name, timestamp, git SHA, and status (current/behind/ahead).

## Typical Flow

```
/checkpoint create "feature-start"
[implement]
/checkpoint create "core-done"
/checkpoint verify "core-done"
[refactor]
/checkpoint create "refactor-done"
/checkpoint verify "feature-start"
```

## Arguments

- `create <name>` - Create named checkpoint
- `verify <name>` - Verify against named checkpoint
- `list` - Show all checkpoints
- `clear` - Remove old checkpoints (keeps last 5)
