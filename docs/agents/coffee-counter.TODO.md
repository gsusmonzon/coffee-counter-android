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
- [ ] Write short MVP spec
- [ ] Write acceptance criteria for add from widget
- [ ] Write acceptance criteria for undo from widget
- [ ] Write acceptance criteria for day rollover
- [ ] Write acceptance criteria for history with empty days
- [ ] Write acceptance criteria for persistence after reboot/app restart

## UX
- [ ] Sketch 2x1 widget layout
- [ ] Define tap zones: main add area, small undo area, optional open app area
- [ ] Define visual states for widget
- [ ] Define vibration behavior
- [ ] Define history screen layout
- [ ] Define 7-day and 30-day stats views
- [ ] Define empty state for first use

## Technical design
- [ ] Choose Android stack
- [ ] Define data model for daily counts
- [ ] Define repository/API for increment, decrement, get today, get stats
- [ ] Define how widget reads and updates state
- [ ] Define local date handling
- [ ] Define synthetic zero generation for stats UI
- [ ] Define app navigation
- [ ] Define vibration integration

## Android project setup
- [ ] Create Android project
- [ ] Configure Kotlin
- [ ] Add Room
- [ ] Add widget framework
- [ ] Add basic app theme
- [ ] Configure min SDK / target SDK

## Data layer
- [ ] Create `DailyCount` entity
- [ ] Create DAO
- [ ] Create database
- [ ] Implement `incrementToday()`
- [ ] Implement `decrementToday()`
- [ ] Implement `getTodayCount()`
- [ ] Implement `getStats(range)`
- [ ] Prevent decrement below 0

## Widget
- [ ] Create 2x1 widget
- [ ] Show today count
- [ ] Add main tap area for `+1`
- [ ] Add small tap area for `-1`
- [ ] Add optional open app tap area
- [ ] Refresh widget after each action
- [ ] Test widget resize and layout

## App UI
- [ ] Create home/activity screen
- [ ] Show today count
- [ ] Show last 7 days
- [ ] Show last 30 days
- [ ] Render missing days as 0
- [ ] Add simple app icon and label

## Feedback
- [ ] Trigger vibration on add
- [ ] Trigger vibration on undo
- [ ] Ensure feedback is fast and not excessive

## Testing
- [ ] Test add increases today count
- [ ] Test undo decreases today count
- [ ] Test undo at 0 stays at 0
- [ ] Test day change shows new day as 0
- [ ] Test previous day remains unchanged
- [ ] Test stats include synthetic 0 days
- [ ] Test persistence after app restart
- [ ] Test persistence after device reboot
- [ ] Test widget updates correctly after actions
- [ ] Test rapid repeated taps

## Release
- [ ] Build debug APK
- [ ] Install on device
- [ ] Validate widget on real launcher
- [ ] Validate one-hand usability
- [ ] Polish labels and spacing
- [ ] Create V1 release

## V2 ideas
- [ ] Edit past days
- [ ] Export CSV
- [ ] Charts
- [ ] Daily goal
- [ ] Backup/import