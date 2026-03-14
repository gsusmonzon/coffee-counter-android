# Code Quality Remediation Plan

Review basis:

- [code-quality-review.codex.md](/Users/jesus/projects/android/CoffeeCounter/docs/agents/code-quality-review.codex.md)
- [code-quality-review.claude.md](/Users/jesus/projects/android/CoffeeCounter/docs/agents/code-quality-review.claude.md)

Scope:

- current codebase state on 2026-03-14
- include current Phase 13 and Phase 14 code
- close correctness, maintainability, and cleanup gaps
- preserve existing product behavior except where a fix intentionally improves correctness or consistency

## Execution Status

- Batch 1: Completed on 2026-03-14
- Batch 2: Completed on 2026-03-14
- Batch 3: Completed on 2026-03-14
- Batch 4: Completed on 2026-03-14
- Batch 5: Completed on 2026-03-14
- Final verification: Completed on 2026-03-14

Verification run:

- `./gradlew testDebugUnitTest --tests com.gsusmonzon.coffeecounter.ui.home.HomeViewModelTest`
- `./gradlew testDebugUnitTest --tests com.gsusmonzon.coffeecounter.widget.CoffeeWidgetActionHandlerTest`
- `./gradlew testDebugUnitTest --tests com.gsusmonzon.coffeecounter.data.repository.RoomCoffeeRepositoryTest`
- `./gradlew testDebugUnitTest`

## Merged Judgment

### Accepted findings

1. Fix the history summary window bug.
Reason:
- this is a real correctness issue in the current codebase
- it can make the 7-day and 30-day cards include counts from older calendar days after chart expansion

2. Make app-side add/remove explicitly refresh the widget.
Reason:
- current write paths are inconsistent
- widget actions and history edits explicitly refresh, but home add/remove does not
- the safer contract is that every cross-surface write refreshes the widget

3. Remove `CoffeeWidgetRepositoryActions`.
Reason:
- it is a pure pass-through over an existing repository interface
- it adds no policy or isolation value

4. Deduplicate `Context.appContainer()`.
Reason:
- this is small but real duplication in two screens

5. Tighten `RoomCoffeeRepository.updateTodayCount()`.
Reason:
- the current read-transform-write path does an avoidable double read
- folding it into one transaction is simpler and more coherent

6. Remove dead and unused code.
Items:
- unused `WidgetColors.onPrimary`
- hardcoded widget `"Today"` label
- dead API-level guard in reminder channel creation
- unused version catalog entry
- placeholder `ExampleUnitTest`
- unused drawables `ic_remove.xml`, `ic_settings.xml`, `ic_substract_one.xml`

### Rejected or revised conclusions from prior reviews

1. Reject: "no gaps requiring new tests"
Reason:
- the summary-window bug is behavior-affecting and needs regression coverage
- explicit widget refresh on app-side add/remove should also be protected by tests

2. Revise: "issues are minor"
Reason:
- most items are minor cleanup
- the summary-window bug is not minor because it can show wrong statistics in the main `Home` UI

## Implementation Strategy

Work in this order:

1. Correctness fixes first.
2. Cross-surface consistency second.
3. Abstraction and duplication cleanup third.
4. Dead code and resource cleanup last.
5. Run regression suites after each logical batch where practical.

This keeps risky behavior fixes isolated from low-risk cleanup.

## Detailed Plan

## Batch 1: Fix history summary correctness

Goal:
- make 7-day and 30-day summary cards always reflect the last 7 and 30 calendar days, regardless of chart pagination

Changes:
- update `HomeViewModel` / `buildHomeUiState()` so summary calculations use explicit date windows, not `storedCounts.takeLast(...)`
- keep chart history and summary history logically separate even if they share the same repository query result
- prefer filtering by `LocalDate` boundaries derived from `today`

Preferred implementation:
- compute `last30Start = today.minusDays(29)`
- compute `last7Start = today.minusDays(6)`
- derive `last30StoredCounts` and `last7StoredCounts` by filtering `storedCounts` against those ranges
- build the 30-day and 7-day timelines from those filtered lists

Why this approach:
- smallest fix with the least architectural churn
- preserves the current single-query structure
- prevents older sparse rows from leaking into summary cards

Tests:
- add `HomeViewModel` test: when chart expands beyond 30 days and history is sparse, the summary cards still ignore rows older than the last 30 calendar days
- keep existing zero-fill summary tests unchanged where still valid

Acceptance:
- expanding chart history must not change the 7-day or 30-day totals unless newly included dates are actually inside those windows

## Batch 2: Make widget refresh behavior consistent

Goal:
- ensure every user-visible write that should affect the widget explicitly refreshes it

Changes:
- add `widgetUpdater.refresh()` after `onAddCoffeeClick()`
- add `widgetUpdater.refresh()` after `onRemoveCoffeeClick()`

Why this approach:
- consistent with widget actions, history edit saves, and reset-all flow
- avoids relying on implicit Glance behavior for repository-driven updates

Tests:
- add `HomeViewModel` tests that verify add/remove call `widgetUpdater.refresh()`
- keep widget action handler tests focused on widget-side writes only

Acceptance:
- manual app logging and widget logging follow the same refresh contract

## Batch 3: Simplify widget action wiring

Goal:
- remove thin abstractions that no longer earn their cost

Changes:
- remove `CoffeeWidgetRepositoryActions`
- inject `CoffeeRepository` directly into `CoffeeWidgetActionHandler`
- update `CoffeeWidgetActionHandler.from(context)` accordingly
- adjust widget handler tests to use a fake `CoffeeRepository`

Why this approach:
- fewer types
- simpler constructor graph
- no loss of testability because `CoffeeRepository` is already an interface

Tests:
- update `CoffeeWidgetActionHandlerTest` to use a fake repository implementing `CoffeeRepository`
- keep assertions on increment/decrement, widget refresh, and feedback behavior

Acceptance:
- widget action handling remains fully covered and easier to trace

## Batch 4: Tighten shared helpers and repository internals

Goal:
- reduce duplication and make write paths easier to reason about

Changes:
- extract one shared `Context.appContainer()` helper near `CoffeeCounterApplication`
- refactor `RoomCoffeeRepository.updateTodayCount()` so current-count read and write live in one transaction helper

Why this approach:
- keeps helper ownership near app composition root
- removes the redundant repository read without changing outward behavior

Tests:
- existing repository tests should continue to pass
- add a dedicated repository test only if the refactor materially changes control flow

Acceptance:
- repository write path is simpler
- UI screens no longer carry duplicated app-container lookup code

## Batch 5: Remove dead code and stale resources

Goal:
- finish the pass by deleting code and assets that add noise

Changes:
- remove `WidgetColors.onPrimary`
- move widget `"Today"` text to `strings.xml`
- remove dead reminder API guard
- remove unused `androidx-compose-material3-adaptive-navigation-suite` version catalog entry
- delete `ExampleUnitTest.kt`
- delete unused drawables

Why this approach:
- all are low-risk cleanup items
- easiest to review after behavior fixes are already stable

Tests:
- no new dedicated tests needed
- rely on compile/test verification

Acceptance:
- no dead symbols or stale placeholder artifacts remain from earlier iterations

## Verification Plan

Unit and integration coverage to run:

- `RoomCoffeeRepositoryTest`
- `HomeViewModelTest`
- `SettingsViewModelTest`
- `CoffeeWidgetActionHandlerTest`
- reminder tests
- full `testDebugUnitTest` suite

Instrumentation to rerun if environment permits:

- `AppFlowsInstrumentedTest`

Manual verification:

- open `Home`, expand chart, confirm 7-day and 30-day cards stay correct
- add and remove coffee from `Home`, confirm widget count updates
- edit a past day, confirm widget still refreshes correctly
- reset all history, confirm app and widget both show zero state

## Risks and Edge Cases

1. Sparse history is the key edge case for the summary fix.
Recent calendar windows with many missing days must still zero-fill correctly.

2. Widget refresh frequency increases slightly.
That is acceptable here because write volume is tiny and immediate feedback matters more than micro-optimization.

3. Refactoring `CoffeeWidgetActionHandlerTest` should not accidentally weaken coverage.
Keep the test assertions behavior-based, not constructor-shape-based.

4. Moving `appContainer()` helper must not create a new abstraction layer.
Use a small shared extension only.

## Definition of Done

- the summary-window bug is fixed and regression-tested
- app-side add/remove explicitly refreshes the widget and is tested
- widget action wiring is simplified
- duplicate helper code is removed
- dead code and unused resources are deleted
- existing regression suites pass when run in a writable environment
