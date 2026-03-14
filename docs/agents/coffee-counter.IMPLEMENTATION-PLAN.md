# Coffee Counter Implementation Plan

Last updated: 2026-03-14

## Goal

Build the MVP in small, verifiable phases:
- one-tap coffee logging from a home screen widget
- automatic daily statistics
- local-only persistence
- zero-friction interaction

Each phase follows the same loop:
- Plan
- Implement
- Test / Verify
- Accept / Reject

## Confirmed Decisions

- Widget technology: `Glance` unless implementation complexity proves higher than expected
- Persistence: `Room`
- App structure: two screens
- `Home`: today counter with manual add plus history
- `Settings`: app version plus reset-all with confirmation
- prefer fast tests over slow tests
- prefer integration tests over narrow component tests
- use unit tests for business logic where they add value
- use instrumentation only when JVM tests cannot cover the behavior
- do not use screenshot tests

## Architecture

Use a minimal Modern Android App Architecture balance:

- UI layer:
- Compose screens
- `ViewModel` per screen
- UI state models exposed from `ViewModel`

- Domain layer:
- no mandatory domain layer for V1
- add focused domain classes only for real business rules, for example:
- generating synthetic zero-filled history ranges
- day/date calculations
- reset-all orchestration if it stops being trivial

- Data layer:
- Room entities, DAO, database
- repository as the app-facing API
- no separate `LocalDataSource` abstraction in V1; the DAO already plays that role

- Widget integration:
- Glance widget and action handling call the same repository used by the app
- widget must refresh immediately after add or undo

Recommended package direction:

- `ui/home`
- `ui/settings`
- `data/local`
- `data/repository`
- `widget`
- `domain` only if needed

## Testing Strategy

Default test pyramid for this project:

- Fast integration tests first:
- repository + Room in-memory database
- `ViewModel` tests with fake or in-memory repository

- Unit tests where they add clear value:
- date/day rollover rules
- history gap-filling logic
- decrement floor at `0`

- Robolectric when Android framework behavior matters but instrumentation is unnecessary:
- widget action handling if practical
- vibration interaction boundaries if isolated cleanly

- Instrumentation only for final, high-value checks:
- widget added to launcher and usable end-to-end
- reset-all confirmation flow if JVM coverage is insufficient

Testing rule for each phase:
- add only the smallest set of tests that protects the new behavior from regression

## Phase Tracker

Status values for implementation:
- `Pending`
- `In progress`
- `Accepted`
- `Rejected`

| Phase | Goal | Status | Completed on |
|---|---|---|---|
| 1 | Project foundation and architecture skeleton | Accepted | 2026-03-14 |
| 2 | Room-backed coffee logging and repository tests | Accepted | 2026-03-14 |
| 3 | Home screen with today counter and manual add | Accepted | 2026-03-14 |
| 4 | History with 7-day and 30-day zero-filled stats | Accepted | 2026-03-14 |
| 5 | Widget add/undo flow with refresh and vibration | Accepted | 2026-03-14 |
| 6 | Settings screen with app version and reset-all | Accepted | 2026-03-14 |
| 7 | Final regression coverage and device validation | In progress | - |
| 8 | Widget setup refinement with in-app add-widget flow | Pending | - |

## Skill Recommendation

Do not create a dedicated project skill at the start.

Reason:
- the project is still settling its real architecture, testing patterns, and widget implementation details
- a skill is more useful once the workflow is stable and reusable rather than still evolving

Recommended checkpoint:
- reassess after Phase 3

Expected trigger for creating a skill:
- the app structure is stable enough
- repository and testing conventions are proven
- the `Home` screen flow is implemented
- the remaining work starts to repeat known patterns

If created after Phase 3, the skill should stay narrow and practical:
- project workflow: `Plan > Implement > Test / Verify > Accept / Reject`
- architecture guardrails for this repo
- testing heuristics: fast integration-first coverage
- widget implementation rules and verification checklist
- release/device validation checklist for later phases

## Phases

## Phase 1

Goal:
- establish the minimal app structure we will build on

Scope:
- clean out placeholder navigation and placeholder screens
- create the two-screen app shell: `Home` and `Settings`
- define package structure
- add dependencies required for Room, lifecycle/ViewModel, navigation if needed, and Glance
- define the core data model shape and repository contract

Implementation notes:
- keep navigation simple; do not overbuild
- decide whether one shared activity with simple navigation is enough for V1
- create repository interfaces only if they help testing or separation; otherwise use a concrete repository

Verification:
- app launches cleanly
- navigation between `Home` and `Settings` works
- no placeholder destinations remain
- project builds

Tests:
- none beyond build verification unless a pure business helper is introduced here

Acceptance:
- accept when the scaffold supports the remaining phases without structural rework

## Phase 2

Goal:
- make coffee logging and persistence work correctly in the data layer

Scope:
- create `DailyCount` Room entity
- create DAO and database
- implement repository methods for:
- increment today
- decrement today with floor at `0`
- get today count
- get date range counts
- reset all data

Implementation notes:
- store counts by local calendar date
- keep date handling explicit and deterministic
- prefer returning streams/state where it simplifies UI updates

Verification:
- increment creates or updates today’s row
- decrement never goes below `0`
- data survives process restart
- previous days remain unchanged

Tests:
- in-memory Room integration tests for repository behavior
- unit tests for date/key mapping only if needed

Acceptance:
- accept when repository behavior is stable enough for UI and widget integration

## Phase 3

Goal:
- deliver a usable `Home` screen with manual logging

Scope:
- build `HomeViewModel`
- show today count
- add manual `+1` action from the app UI
- display a basic history area placeholder if full history is not ready yet

Implementation notes:
- prioritize immediate feedback after tapping add
- keep UI state small and explicit

Verification:
- opening the app shows today’s current count
- tapping add updates count immediately and persists it
- reopening the app shows the persisted count

Tests:
- `ViewModel` tests for add flow and state updates
- reuse repository integration tests from Phase 2 rather than duplicating persistence coverage

Acceptance:
- accept when the app can be used without the widget for core logging

Post-phase note:
- after Phase 3 is accepted, decide whether the project has enough stable conventions to justify creating a dedicated local skill

## Phase 4

Goal:
- provide automatic daily statistics in the app

Scope:
- implement last 7 days and last 30 days history
- generate synthetic `0` values for missing days
- render history clearly on `Home`

Implementation notes:
- keep history rendering simple and readable
- avoid charts in V1
- isolate zero-fill and date-range logic if that makes testing cleaner

Verification:
- days with no stored rows show `0`
- the current day appears correctly in both ranges
- crossing into a new day shows a new count of `0` without mutating prior days

Tests:
- unit tests for zero-fill/range generation logic
- repository or `ViewModel` tests for history output

Acceptance:
- accept when history behavior matches the MVP spec, especially synthetic zero days

## Phase 5

Goal:
- deliver the widget-first logging experience

Scope:
- add 2x1 Glance widget
- show today count
- tap main area to add `+1`
- tap small undo area to subtract `-1`
- refresh widget immediately after action
- add vibration feedback on add and undo

Implementation notes:
- keep widget layout optimized for speed and readability
- share repository logic with the app to avoid duplicated rules
- if Glance creates avoidable friction for the MVP, reassess early before the implementation spreads

Verification:
- widget can be added to the home screen
- tapping add updates today count immediately
- tapping undo decrements but never below `0`
- widget remains usable without opening the app

Tests:
- keep logic coverage in repository tests
- add Robolectric tests only where widget action behavior can be covered meaningfully
- defer full launcher/device validation to the final phase

Acceptance:
- accept when the primary MVP promise, one-tap widget logging, works reliably

## Phase 6

Goal:
- complete the minimal second screen

Scope:
- build `Settings` screen
- show app version
- add reset-all action
- add confirmation dialog before reset

Implementation notes:
- reset-all must update app UI and widget state consistently
- keep settings intentionally sparse

Verification:
- app version is visible
- reset requires confirmation
- confirming reset clears data and returns today/history to `0`
- widget reflects reset state

Tests:
- `ViewModel` or repository tests for reset behavior
- UI tests only if dialog behavior is awkward to cover otherwise

Acceptance:
- accept when reset-all is safe, explicit, and consistent across surfaces

## Phase 7

Goal:
- lock down regressions and validate the product on real Android behavior

Scope:
- review test gaps
- add only the missing high-value tests
- perform device/emulator validation for widget behavior, day rollover assumptions, and reset flow
- clean up naming, strings, and spacing

Implementation notes:
- this phase is for confidence, not feature expansion
- avoid broad UI test suites with low signal

Verification:
- build debug APK successfully
- key flows pass on device or emulator:
- app add flow
- history rendering
- widget add/undo
- reset-all

Tests:
- selective instrumentation tests only for flows that cannot be covered sufficiently with JVM tests

Acceptance:
- accept when the MVP is stable enough for regular use without obvious regressions

## Phase 8

Goal:
- make widget setup easier from inside the app

Scope:
- research and implement an in-app add-widget entry point
- prefer direct widget pinning over trying to open the generic widgets picker
- add a simple UI entry point from the app, likely from `Home` or `Settings`
- handle unsupported launchers gracefully
- add lightweight guidance text if direct pinning is unavailable

Implementation notes:
- Android does not provide a standard public API to open the launcher's generic widgets screen
- the supported direction is requesting that the launcher pin this app's widget directly
- if using Glance APIs is reliable in this project version, prefer the Glance pinning path
- otherwise use the platform `AppWidgetManager.requestPinAppWidget(...)` flow

Verification:
- on supported launchers, tapping the in-app CTA opens the system pin-widget confirmation flow
- confirming the flow adds the widget successfully
- on unsupported launchers, the app shows a clear fallback message instead of failing silently

Tests:
- keep this light
- unit test availability/fallback logic if extracted
- rely mainly on device/emulator verification because launcher support is environment-specific

Acceptance:
- accept when a user can start widget setup from inside the app with a clear success or fallback path

## Phase Order Rationale

- Phase 1 creates a stable skeleton without premature abstraction.
- Phase 2 puts the persistence and business rules in place early.
- Phase 3 gives a usable app flow before widget complexity.
- Phase 4 completes the core statistics behavior.
- Phase 5 adds the main differentiator: one-tap widget logging.
- Phase 6 finishes the minimal secondary screen.
- Phase 7 adds only the final confidence work that is actually needed.
- Phase 8 improves onboarding into the widget-first experience without expanding the MVP core.

## Working Agreement For Implementation

For each phase, do not start coding immediately. First confirm:
- the goal of the phase
- the exact scope for that phase
- any missing constraints or edge cases

Then execute:
- Plan
- Implement
- Test / Verify
- Accept / Reject

After each phase:
- update the tracker status
- fill `Completed on` when a phase is accepted or rejected
