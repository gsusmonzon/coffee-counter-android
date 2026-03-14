# CoffeeCounter Agent Context

## Product Goal
- Build a minimal Android app for logging coffee intake with one tap from a home screen widget.
- Keep daily statistics automatically.
- Optimize for zero friction logging and no configuration.
- Build iteratively.

## Current Project Context
- Android app in Kotlin with Jetpack Compose.
- Current scaffold is mostly placeholder UI.
- `minSdk = 31`
- `targetSdk = 36`

## Source Notes
- Product notes live in @docs/agents/coffee-counter.SPECS.md
- Exploration backlog lives in @docs/agents/coffee-counter.TODO.md
- Keep notes for future referene under @docs/agents/
- Use those files as context, but only pull in what is needed for the current step.

## Working Rules
- Ask until you understand the idea completely: goals, constraints, tech preferences, and edge cases.
- Do not add configuration or optional complexity unless explicitly requested.
- Prefer the smallest implementation that advances the current iteration.
- Keep data local-first and offline-first unless requirements change.

