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
- Keep notes for future reference under @docs/agents/
- Use those files as context, but only pull in what is needed for the current step.

## Working Rules
- Ask until you understand the idea completely: goals, constraints, tech preferences, and edge cases.
- Do not add configuration or optional complexity unless explicitly requested.
- Prefer the smallest implementation that advances the current iteration.
- Keep data local-first and offline-first unless requirements change.

## Architecture Direction
- Follow a minimal Modern Android App Architecture style.
- UI layer: Compose screens plus `ViewModel` state holders.
- Data layer: Room `DAO` plus repository.
- Treat the Room `DAO` as the local data source for V1. Do not add a separate `LocalDataSource` abstraction unless it solves a concrete problem.
- Add domain/use case classes only when business logic becomes non-trivial or needs isolated testing. Avoid thin pass-through use cases.
- Keep widget actions and app screens on the same shared repository and data model.

## V1 Product Shape
- Two screens only.
- `Home`: today counter with manual add, plus history sections.
- `Settings`: app version plus reset-all action with confirmation.
