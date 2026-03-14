# Coffee Counter — MVP Specification

## Purpose

A minimal Android app that allows logging coffee intake with **one tap from a home screen widget**, while keeping **daily statistics automatically**.

The design goal is **zero friction logging** and **no configuration**.

---

# Product Principles

- One coffee = one tap
- Widget-first interaction
- No setup required
- No manual reset
- Instant feedback
- Local-only storage
- Extremely small scope

---

# MVP Scope (V1)

Included:

- 2x1 home screen widget
- Tap main area to add one coffee
- Small undo area to subtract one coffee
- Vibration feedback on add and undo
- Today’s count visible in widget
- Automatic day rollover
- Local data persistence
- History screen
- Last 7 and 30 days statistics
- Empty days shown as `0`

Excluded:

- Editing past days
- Export
- Cloud sync
- Multiple drink types
- Caffeine estimation
- Goals or reminders
- Charts

---

# Functional Requirements

## Coffee Logging

The system shall allow the user to register one coffee with a single tap.

The system shall increment the current day count by `1` when the add action is triggered.

The system shall allow the user to undo one coffee using the widget undo control.

The system shall decrement the current day count by `1` when undo is triggered.

The system shall not allow the count to go below `0`.

---

## Daily Behavior

The system shall store coffee counts per **local calendar day**.

The system shall automatically start a new day with count `0` when the device date changes.

The system shall preserve previous days’ counts.

The system shall generate **synthetic `0` days in the statistics UI** when no data exists for that day.

---

## Widget

The system shall provide a **home screen widget**.

The widget shall support **2x1 size**.

The widget shall display:

- today's count
- a main tap area for `+1`
- a small undo area for `-1`

The widget shall refresh immediately after each action.

The widget shall provide **vibration feedback** after each add or undo action.

The widget shall remain usable without opening the app.

---

## Statistics

The app shall provide a **history screen**.

The history screen shall display:

- the last **7 days**
- the last **30 days**

Days without recorded data shall be displayed as `0`.

---

# Non-Functional Requirements

- The app shall work **offline**.
- The app shall store data **locally on the device**.
- The app shall not require login or account.
- Logging a coffee shall require **only one tap**.
- Widget feedback shall be **immediate**.
- The widget shall remain readable in **2x1 layout**.

---

# Data Model (Conceptual)

Each row represents one day.

```

## DailyCount

date (YYYY-MM-DD)
count (integer >= 0)

```

Example:

| date       | count |
|------------|------|
| 2026-03-13 | 4    |
| 2026-03-14 | 2    |

Missing rows are interpreted as **0 in statistics**.

---

# Widget Interaction Model

```

+-----------------------+
|        3              |
|                       |
|  (+ tap anywhere)     |
|                       |
|        [-]            |
+-----------------------+

```

Behavior:

- Tap main area → `+1`
- Tap undo area → `-1`
- Vibration triggered on action
- Widget refreshes immediately

Undo rules:

- Only affects today
- Cannot go below `0`

---

# Acceptance Criteria

## AC1 — Add Coffee from Widget

**Given**

Today’s coffee count is `3`.

**When**

The user taps the main widget area.

**Then**

The system increments today's count to `4`.

**And**

The widget refreshes immediately.

**And**

A vibration feedback occurs.

---

## AC2 — Undo Coffee from Widget

**Given**

Today’s coffee count is `3`.

**When**

The user taps the widget undo area.

**Then**

The system decrements today's count to `2`.

**And**

The widget refreshes immediately.

**And**

A vibration feedback occurs.

---

## AC3 — Prevent Negative Counts

**Given**

Today’s coffee count is `0`.

**When**

The user taps the undo area.

**Then**

The count remains `0`.

**And**

The widget refreshes.

---

## AC4 — Day Rollover

**Given**

Yesterday’s coffee count is `5`.

**When**

The device date changes to a new day.

**Then**

The system shows today's count as `0`.

**And**

Yesterday’s count remains `5` in history.

---

## AC5 — Statistics with Empty Days

**Given**

No coffee was recorded on Tuesday.

**When**

The user opens the statistics screen.

**Then**

Tuesday appears in the list with value `0`.

---

## AC6 — Persistence

**Given**

The user logged coffees today.

**When**

The app restarts or the device reboots.

**Then**

The counts remain unchanged.

---

# Future Versions

## V2

- Edit past days
- Export data (text, json or csv)
- Alarm for late log: we are building an habit of log coffees. Set a daily alarm at 10AM(?) if th euser has logged nothing yet. Not time sensitive
- i18: localize in TOP 5 languages first. At least EN, ES
- Review best practices

Possible later improvements:

- Charts
- Daily goal
- Backup/import
