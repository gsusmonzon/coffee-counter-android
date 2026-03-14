# Coffee Counter TODO

## Product
- [x] Define product goal
- [x] Define V1 scope
- [x] Decide widget interaction model
- [x] Decide feedback for V1: vibration only
- [x] Decide empty days in stats: synthetic 0
- [x] Decide export: V2
- [x] Decide edit past days: not V1

## Requirements
- [x] Write short MVP spec
- [x] Write acceptance criteria for add from widget
- [x] Write acceptance criteria for undo from widget
- [x] Write acceptance criteria for day rollover
- [x] Write acceptance criteria for history with empty days
- [x] Write acceptance criteria for persistence after reboot/app restart

## UX
- [x] Sketch 2x1 widget layout
- [x] Define tap zones: main add area, small undo area, optional open app area
- [ ] Define visual states for widget
- [x] Define vibration behavior
- [x] Define history screen layout
- [x] Define 7-day and 30-day stats views
- [ ] Define empty state for first use

## Technical design
- [x] Choose Android stack
- [x] Define data model for daily counts
- [x] Define repository/API for increment, decrement, get today, get stats
- [x] Define how widget reads and updates state
- [x] Define local date handling
- [x] Define synthetic zero generation for stats UI
- [x] Define app navigation
- [x] Define vibration integration

## Android project setup
- [x] Create Android project
- [x] Configure Kotlin
- [x] Add Room
- [x] Add widget framework
- [x] Add basic app theme
- [x] Configure min SDK / target SDK

## Data layer
- [x] Create `DailyCount` entity
- [x] Create DAO
- [x] Create database
- [x] Implement `incrementToday()`
- [x] Implement `decrementToday()`
- [x] Implement `getTodayCount()`
- [x] Implement `getStats(range)`
- [x] Prevent decrement below 0

## Widget
- [x] Create 2x1 widget
- [x] Show today count
- [x] Add main tap area for `+1`
- [x] Add small tap area for `-1`
- [ ] Add optional open app tap area
- [x] Refresh widget after each action
- [x] Test widget resize and layout

## App UI
- [x] Create home/activity screen
- [x] Show today count
- [x] Show last 7 days
- [x] Show last 30 days
- [x] Render missing days as 0
- [x] Add simple app icon and label

## Feedback
- [x] Trigger vibration on add
- [x] Trigger vibration on undo
- [x] Ensure feedback is fast and not excessive

## Testing
- [x] Test add increases today count
- [x] Test undo decreases today count
- [x] Test undo at 0 stays at 0
- [x] Test day change shows new day as 0
- [x] Test previous day remains unchanged
- [x] Test stats include synthetic 0 days
- [x] Test persistence after app restart
- [x] Test persistence after device reboot
- [x] Test widget updates correctly after actions
- [ ] Test rapid repeated taps

## Release
- [x] Build debug APK
- [x] Install on device
- [x] Validate widget on real launcher
- [x] Validate one-hand usability
- [x] Polish labels and spacing
- [ ] Create V1 release

## V2 ideas
- [ ] Edit past days
- [ ] Export CSV
- [ ] Charts
- [ ] Daily goal
- [ ] Backup/import
