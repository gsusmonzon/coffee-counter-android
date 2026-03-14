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
- `Settings`: add-widget entry point, app version, and delete-all-history with confirmation
- history averages are intentionally based on active coffee days, not full calendar ranges
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
- delete-all-history orchestration if it stops being trivial

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
- delete-all-history confirmation flow if JVM coverage is insufficient

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
| 6 | Settings screen with app version and delete-all-history | Accepted | 2026-03-14 |
| 7 | Final regression coverage and device validation | Accepted | 2026-03-14 |
| 8 | Widget setup refinement with in-app add-widget flow | Accepted | 2026-03-14 |
| 9 | Automatic local-day rollover for app and widget | Accepted | 2026-03-14 |
| 10 | UI review and polish pass | Accepted | 2026-03-14 |
| 11 | Late-log reminder notification | Accepted | 2026-03-14 |
| 12 | Code quality review and simplification pass | Pending | - |
| 13 | Charted history exploration screen | Accepted | 2026-03-14 |
| 14 | Edit past days from chart | Accepted | 2026-03-14 |

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
- delete all data

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
- add delete-all-history action
- add confirmation dialog before delete

Implementation notes:
- delete-all-history must update app UI and widget state consistently
- keep settings intentionally sparse

Verification:
- app version is visible
- delete-all-history requires confirmation
- confirming delete-all-history clears data and returns today/history to `0`
- widget reflects reset state

Tests:
- `ViewModel` or repository tests for delete-all-history behavior
- UI tests only if dialog behavior is awkward to cover otherwise

Acceptance:
- accept when delete-all-history is safe, explicit, and consistent across surfaces

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
- delete-all-history

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

## Phase 9

Goal:
- make day rollover behave correctly without requiring the user to reopen the app or recreate the widget

Scope:
- update app state observation so `Home` rolls over automatically at local midnight
- update widget state observation so the widget shows the new day count at local midnight
- ensure history windows also shift forward to the new local day
- preserve prior days exactly as stored

Implementation notes:
- this is about passive rollover, not only correct writes after midnight
- treat local calendar day boundaries as the source of truth
- prefer a small, explicit time/day source rather than scattering midnight logic through UI code

Verification:
- when local time crosses from `23:59` to `00:00`, app today count becomes `0` if no row exists for the new day
- the widget also shows `0` for the new day without needing manual refresh from the user
- history ranges shift forward by one day and still show prior stored days correctly
- taps after midnight write to the new day only

Tests:
- add focused tests for observer rollover behavior across a mocked local day change
- add only the minimum additional integration coverage needed to prove app and widget behavior

Acceptance:
- accept when midnight rollover behaves correctly in both app and widget under local time semantics

## Phase 10

Goal:
- review and polish the user interface without expanding product scope

Scope:
- review wording, spacing, alignment, typography hierarchy, and card/button consistency
- improve empty or first-use presentation if needed
- review widget readability and balance in the supported size
- keep all existing behavior unchanged unless a polish fix is clearly required

Implementation notes:
- prefer small, high-signal UI changes over redesign
- preserve the intentional minimal feel of the product
- do not add optional settings or decorative complexity

Verification:
- main screens look intentional and consistent on phone sizes used for validation
- widget remains readable and tap targets remain clear
- first-use and low-data states feel deliberate rather than placeholder-like

Tests:
- rely mainly on manual review unless a polish change introduces behavior worth protecting

Acceptance:
- accept when the UI feels cohesive, minimal, and production-ready for V1

## Phase 11

Goal:
- add a lightweight daily reminder that nudges the user to log coffee only when they have not logged anything yet that day

Scope:
- schedule one approximate local-time reminder for every day at `10:00 AM`
- keep the feature always enabled with no user-facing toggle in V1
- when the reminder fires, show a notification only if today’s count is still `0`
- send at most one reminder per day with no snooze or repeated nudges
- tapping the notification opens the app
- if notifications are blocked or permission is denied, show guidance in `Settings`
- place that guidance after the widget card

Implementation notes:
- this reminder is habit support, not a precise alarm; prefer approximate scheduling
- treat local calendar day semantics as the source of truth
- avoid adding notification customization or scheduling settings in V1
- Android 13+ notification permission handling should degrade gracefully

Verification:
- on a day with no logged coffee, one reminder can appear around `10:00 AM` local time
- if at least one coffee is already logged before the reminder check runs, no notification appears
- the reminder does not repeat later the same day
- tapping the notification opens the app
- if notifications are blocked or denied, `Settings` shows guidance in the intended position

Tests:
- add focused tests for the “logged nothing yet today” decision rule
- add only the minimum Android coverage needed for notification scheduling and blocked-permission guidance

Acceptance:
- accept when the reminder is low-friction, once per day, and silent on days where the user already logged coffee

## Phase 12

Goal:
- review the implementation for unnecessary complexity and simplify where it improves maintainability

Scope:
- review architecture seams, naming, duplication, and state ownership
- remove or simplify code that no longer earns its abstraction cost
- tighten comments and documentation where the current implementation decisions should be preserved
- keep external behavior stable

Implementation notes:
- favor deletion and simplification over new abstractions
- preserve tested behavior and avoid churn for cosmetic refactors
- if a pattern is already clear and cheap, leave it alone

Verification:
- project still builds and the relevant test suites still pass
- no user-visible regressions are introduced
- code paths for app, widget, and settings remain easy to trace

Tests:
- rerun existing regression suites
- add tests only if a simplification changes behavior boundaries

Acceptance:
- accept when the codebase is simpler and easier to maintain without reducing product confidence

## Phase 13

Goal:
- make history easier to inspect by adding a charted view of daily coffee counts

Scope:
- open a charted history surface when either history card on `Home` is tapped
- consider both a full-screen screen and a bottom sheet, and choose the simpler option that keeps the chart usable
- both `Home` history cards open the same chart experience
- start with a bar chart that fits roughly `8` days across the available width
- chart axes represent:
- `x`: local calendar day
- `y`: coffee count for that day
- prefer using a chart library first rather than building a custom chart immediately
- load only the most recent N days in memory by default
- support horizontal scrolling backward into older days and forward again toward today
- load older days incrementally as the user scrolls rather than loading all history upfront

Implementation notes:
- the chart should still represent zero-count days explicitly, even when no stored row exists
- keep the first version readable and practical rather than visually ambitious
- if the chart library adds disproportionate complexity, reassess early and document the fallback direction
- preserve local-day semantics and existing history rules

Verification:
- tapping either history card opens the charted history surface
- the initial viewport shows recent days with approximately `8` day columns visible
- users can scroll backward to older days and forward again toward today
- additional past days load as needed while scrolling
- displayed bar heights match stored counts and synthetic zero days consistently

Tests:
- add focused tests for paged/incremental history loading if that logic is extracted
- add only the minimum UI coverage needed for opening the chart surface and basic range loading behavior

Acceptance:
- accept when a user can inspect daily coffee counts over time in a scrollable bar chart without loading the full history eagerly

## Phase 14

Goal:
- allow correcting coffee counts for specific days directly from the charted history view

Scope:
- from the chart screen, allow tapping a day to edit that day’s coffee count
- consider both an editor dialog and an inline editor under the chart, and choose the simpler option
- allow setting the exact count for a tapped day
- allow setting the count to `0`, which clears that day
- do not allow negative counts
- zero-count days must still be tappable even when there is no visible bar
- represent each day with a tappable interaction zone even if the visible bar height is zero
- allow editing `today` only if it does not introduce meaningful extra complexity
- if editing `today` is included, app state and widget state must refresh consistently

Implementation notes:
- editing zero days may require separating visible bar rendering from the tap target for each day
- prefer preserving the chart interaction model rather than overloading bars with too many gestures
- keep the first version explicit and low-friction rather than highly optimized

Verification:
- tapping a non-zero day opens the editor and updates that day correctly
- tapping a zero day also opens the editor and can create a stored count for that day
- setting a day to `0` removes or clears that day correctly
- if `today` is editable in the chosen implementation, widget and app state stay in sync after edits
- chart visuals update immediately after an edit

Tests:
- add focused tests for exact-count updates, zero-clearing behavior, and any today/widget synchronization rules that are introduced
- add UI coverage only where the edit flow would otherwise be easy to regress

Acceptance:
- accept when users can reliably correct or clear individual days from the chart without breaking history consistency

## Phase Order Rationale

- Phase 1 creates a stable skeleton without premature abstraction.
- Phase 2 puts the persistence and business rules in place early.
- Phase 3 gives a usable app flow before widget complexity.
- Phase 4 completes the core statistics behavior.
- Phase 5 adds the main differentiator: one-tap widget logging.
- Phase 6 finishes the minimal secondary screen.
- Phase 7 adds only the final confidence work that is actually needed.
- Phase 8 improves onboarding into the widget-first experience without expanding the MVP core.
- Phase 9 closes the remaining correctness gap around passive midnight rollover.
- Phase 10 polishes the user-facing surfaces once the functional scope is stable.
- Phase 11 adds the late-log reminder once the core logging loop and settings surface are already stable.
- Phase 12 simplifies the implementation after behavior and UI decisions have settled.
- Phase 13 expands history exploration once the core app loop is stable and documented.
- Phase 14 adds corrective editing only after the chart interaction model exists.

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
