# Model Route Command

Recommend the best model tier for the current task by complexity and budget.

## Usage

`/model-route [task-description] [--budget low|med|high]`

## Routing Table

| Model | Use When | Cost |
|-------|----------|------|
| **Haiku 4.5** | Lightweight agents, simple automation, pair programming, frequent invocation in multi-agent systems | 3x cheaper than Sonnet |
| **Sonnet 4.6** | Main development work, implementation, refactoring, complex coding, orchestrating multi-agent workflows | Default |
| **Opus 4.6** | Architecture decisions, deep system design, ambiguous requirements, maximum reasoning tasks | Most expensive |

## Decision Heuristic

- Deterministic mechanical change → **Haiku**
- Standard implementation/refactor → **Sonnet**
- Architecture, deep review, ambiguous requirements → **Opus**
- High-frequency worker agent → **Haiku**
- One-off complex analysis → **Opus**

## Required Output

```
Recommended: [model name]
Confidence: [High/Medium/Low]
Why: [1-2 sentences]
Fallback: [model if first attempt fails]
Estimated cost: [relative: cheap/moderate/expensive]
```

## Arguments

- `[task-description]` — optional free-text description of the task
- `--budget low|med|high` — optional budget constraint
