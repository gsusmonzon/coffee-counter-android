# Code Quality Review — Phase 12

Review scope: current project state as of 2026-03-14, including the Phase 13 and Phase 14 additions already present in the codebase.

## Overall Assessment

The project is still structurally sound for its size:

- one Room-backed repository remains the single source of truth
- the widget, app screens, and reminder flow all route through the same data model
- abstractions are mostly right-sized and test-oriented rather than speculative

The main issue is no longer architectural overgrowth. It is localized drift: one real summary bug introduced by the chart expansion work, one widget-sync risk caused by inconsistent refresh behavior, and several low-cost cleanup items that Phase 12 should remove.

## Findings

### 1. Summary cards can count days outside the last 30 calendar days

Files:
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt#L139)
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt#L516)

Severity: High

`HomeViewModel` widens the repository query range to `maxOf(30, chartDays)` once the chart loads older history. `buildHomeUiState()` then computes the 30-day summary from `storedCounts.takeLast(HISTORY_DAYS_30)`.

That is only correct when there is a stored row for nearly every day. With sparse history, `takeLast(30)` selects the last 30 stored rows, not the rows that fall inside the last 30 calendar days. Older counts can therefore leak into the 7-day and 30-day cards after the user expands the chart.

Recommended fix:
- derive the summary inputs from explicit date filtering against `today.minusDays(29)` and `today.minusDays(6)`, or
- query the 30-day window separately from the chart window

Recommended test:
- add a `HomeViewModel` regression test where `chartDays > 30` and stored rows older than 30 days exist with gaps in the recent window

### 2. Manual add/remove from the app does not explicitly refresh the widget

Files:
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt#L167)
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt#L222)
- [app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt#L222)

Severity: Medium

`onAddCoffeeClick()` and `onRemoveCoffeeClick()` write to the repository only. In the same codebase, widget actions and history edits both call `widgetUpdater.refresh()` after writes.

That asymmetry matters because the widget implementation already assumes explicit refresh triggers for action handling and rollover. If Glance does not fully propagate repository flow changes on its own here, the widget can stay stale after app-side logging until some later refresh path runs.

Recommended fix:
- refresh the widget after app-side add/remove, or
- prove that Glance reacts correctly without explicit updates and lock that behavior down with a test

### 3. `CoffeeWidgetRepositoryActions` no longer earns its abstraction cost

File:
- [app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt#L217)

Severity: Low

`CoffeeWidgetActionHandler` depends on a two-method wrapper over `CoffeeRepository`, which is already an interface. The wrapper does not isolate policy or platform behavior; it only forwards calls.

Recommended fix:
- inject `CoffeeRepository` directly into `CoffeeWidgetActionHandler`

### 4. `Context.appContainer()` is duplicated

Files:
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/home/HomeScreen.kt#L500)
- [app/src/main/java/com/gsusmonzon/coffeecounter/ui/settings/SettingsScreen.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/ui/settings/SettingsScreen.kt#L377)

Severity: Low

The same one-line extension exists in two files. This is exactly the kind of small duplication Phase 12 should collapse.

Recommended fix:
- move it to a shared internal helper near `CoffeeCounterApplication`

### 5. Repository update path reads today twice

File:
- [app/src/main/java/com/gsusmonzon/coffeecounter/data/repository/RoomCoffeeRepository.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/data/repository/RoomCoffeeRepository.kt#L84)

Severity: Low

`updateTodayCount()` reads the current row before entering `setCount()`, and `setCount()` reads again inside the transaction. The behavior is fine in practice for this app, but the first read is redundant and slightly weakens the transaction story.

Recommended fix:
- move read-transform-write into a single transaction helper

## Low-Cost Cleanup

- Remove the dead `WidgetColors.onPrimary` value in [app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt#L300).
- Replace the hardcoded `"Today"` label in [app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/widget/CoffeeCounterWidget.kt#L122) with a string resource.
- Delete the unreachable API guard in [app/src/main/java/com/gsusmonzon/coffeecounter/reminder/LateLogReminder.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/main/java/com/gsusmonzon/coffeecounter/reminder/LateLogReminder.kt#L196) because `minSdk = 31`.
- Remove the unused version catalog entry in [gradle/libs.versions.toml](/Users/jesus/projects/android/CoffeeCounter/gradle/libs.versions.toml#L33).
- Delete the placeholder test in [app/src/test/java/com/gsusmonzon/coffeecounter/ExampleUnitTest.kt](/Users/jesus/projects/android/CoffeeCounter/app/src/test/java/com/gsusmonzon/coffeecounter/ExampleUnitTest.kt#L12).
- Delete unused drawables:
  - `app/src/main/res/drawable/ic_remove.xml`
  - `app/src/main/res/drawable/ic_settings.xml`
  - `app/src/main/res/drawable/ic_substract_one.xml`

## Leave Alone

These patterns were reviewed and still look justified:

- `CoffeeRepository` as the app-facing interface
- `LocalDateProvider` for rollover testing
- pure domain helpers for timeline and summary generation
- manual DI through `AppContainer`
- `CoffeeWidgetUpdater` and `CoffeeWidgetFeedbackPerformer`
- separate `HistoryChartBottomSheet` and `HistoryEditDialog` files instead of inflating `HomeScreen.kt` further

## Verification Notes

- Static review completed against the current source tree.
- The repo currently has 11 test files and 46 test methods by source inspection.
- I could not run Gradle verification in this environment because the sandbox is read-only and blocks build output creation.

