# android-app-project
[View Trello Board](https://trello.com/b/2uEvflKy/title-here)
## Design

### Principles
- One message and at most two actions per screen
- Large text and buttons, single-tap actions only (no swipes or long presses)
- Colour is never the only signal: every state also has a word and an icon
- Red is reserved for "I need help" and alarms

### Colours
| Role | Hex | Use |
| --- | --- | --- |
| Main text | `#1A1A1A` | Body text and headings |
| Secondary text | `#4A4A4A` | Labels and times |
| Primary action | `#1F4E9C` | Main buttons |
| All well | `#1E6B3A` / `#E7F3EC` | Status and success |
| Help / alarm | `#B3261E` | Help button and alarm only |
| Warning | `#FFE8A3` | Paused checks, low battery |

All text pairings meet WCAG 2.1 AA contrast (4.5:1 or higher).

### Typography
Roboto (Android default), sized in `sp` so it scales with the system font setting:
Status 32sp bold · Heading 26sp bold · Button 22sp bold · Body 20sp · Small 18sp (minimum).

### Sizes
Buttons are at least 64dp tall (96dp for "I need help"), with 16dp between touch targets, 24dp screen margins and 16dp rounded corners.

### In code
The theme lives in `ui/theme/`. Wrap screens in `DailyOkTheme { … }` and use the shared components (`DailyOkButton`, `HelpButton`, `StatusCard`, `WarningBanner`) instead of styling elements by hand.
![dailyok-style.png](dailyok-style.png)

## Daily checks

DailyOK checks on the person twice a day and starts the "Are you OK?" flow (prompt → alarm → SMS) when it sees no signs of normal life.

| Check | Rule (defaults) |
| --- | --- |
| Morning check | No activity since waking (07:00) by the morning check time (10:00) → prompt |
| Daytime check | No activity for the inactivity period (4 h, adjustable 2–6 h) between waking and bedtime (22:00) → prompt |

- Sleep time never counts as inactivity, and nothing runs after bedtime.
- The home screen's pause switch turns all checks off.
- After a prompt, there is no new prompt until the person is active again.
- **Activity** = unlocking the phone, unplugging the charger, or pressing "I'm OK" (on the home screen or the prompt).

### How it works

| File | Role |
| --- | --- |
| `domain/CheckEngine.kt` | The rules as a pure Kotlin function: `evaluate()` returns `AllWell` or `Prompt(reason)`, `nextCheckTime()` says when to look again. No Android code. |
| `data/CheckStore.kt` | Saves last activity, last prompt and check times on the phone (`app_prefs`, shared with the home screen). |
| `platform/check/CheckScheduler.kt` | Sets an alarm for the next check; when it fires, runs the engine and starts the prompt. |
| `platform/check/CheckAlarmReceiver.kt` | Receives the alarm, works with the screen off and the app closed. |
| `platform/check/ActivityTracker.kt` | Records activity and moves the next check. |
| `platform/check/CheckNotifier.kt` | Full-screen "Are you OK?" notification when the app is closed. |

Tests: `app/src/test/.../domain/CheckEngineTest.kt` covers a normal day, a lie-in, a nap, sleep hours, pause and demo inactivity periods. Run it from Android Studio.

**Permissions:** notifications (asked on first start), full-screen intent, exact alarms. On Android 14 exact alarms are off by default, so a check can fire a few minutes late unless *Alarms & reminders* is allowed for the app.

**Known limits:** unlocks are only heard while the app's process is running (until the background service from "Resume after restart" is added), and pickups are not counted yet (`PickupDetector` should call `ActivityTracker.record()`).

## Demo settings

*Settings → Demo* holds mock values for presentations; changes apply at once and are saved on the phone.

| Setting | Range (default) |
| --- | --- |
| Demo mode | On / off (on). Off = real timings: prompt 10 min, alarm 15 min |
| Prompt waits before alarm | 5–120 s (15 s) |
| Alarm countdown before SMS | 10–300 s (30 s) |
| Inactivity period (daytime check) | Normal, 30 s, 1, 2, 5, 10, 30 or 60 min (Normal = the 2–6 h setting) |

Buttons: **Simulate a missed morning**, **Simulate inactivity** (pick a short inactivity period first), **Run the check now**, and **Reset check times to normal** (use after simulating a missed morning). A status panel shows the check times, last activity, last prompt and next check.

Hidden trigger: when demo mode is on, long-press the top-right corner of any screen to start the prompt or the alarm.

Before real users get the app, turn demo mode off or remove `demoSection = { DemoSection() }` from `SettingsRoute`.




## Phone usage log (last phone activity)

The home screen also shows **Last phone activity**: the most recent time the phone was woken or unlocked, read from Android's own usage log. It works even if DailyOK was never opened in between.

| Setting | Value |
| --- | --- |
| Source | Android usage stats (`UsageStatsManager`) |
| Events counted | Screen on (`SCREEN_INTERACTIVE`) and keyguard hidden (`KEYGUARD_HIDDEN`) |
| Sync interval | Every 12 h (approximate, Android may delay it) |
| Lookback window | 24 h per run, so a delayed or skipped run loses nothing |
| Also runs | Each time the home screen resumes |

A new value is saved only if it is newer than the stored one, so the overlapping windows are harmless.

### How it works

| File | Role |
| --- | --- |
| `domain/interfaces/ActivitySource.kt` | Contract: `hasPermission()` and `lastActivityBetween(from, to)`. |
| `domain/interfaces/ActivityRepository.kt` | Contract: `lastActivity` flow and `save(time)`. |
| `domain/useCase/SyncLastActivity.kt` | Asks the source for the newest event in the last 24 h and saves it if newer. No Android code. |
| `data/repository/DataStoreActivityRepository.kt` | Stores the last activity time in DataStore (`activity`). |
| `platform/usage/UsageActivitySource.kt` | Reads the usage log and checks the permission. |
| `platform/worker/ActivitySyncWorker.kt` | WorkManager job that runs `SyncLastActivity`. |
| `platform/worker/ActivityScheduler.kt` | Enqueues the 12 h periodic job (unique work, `KEEP`). |
| `platform/worker/AppWorkerFactory.kt` | Gives the worker its dependencies. |
| `AppContainer.kt` | Creates the shared source, repository and use case once. Held by `DailyOkApp`. |
| `ui/screens/home/HomeViewModel.kt` | Observes the stored value and calls `refresh()` on resume. |

**Permission:** *Usage access* (`PACKAGE_USAGE_STATS`). It cannot be granted with a dialog. The home screen shows a "Grant usage access" button that opens *Settings > Usage access*. Until it is granted, the sync does nothing.

**Android versions:** the usage events need Android 9 (API 28) and the permission check differs before Android 10 (API 29). On Android 7 to 8 (minSdk is 24) the usage log returns nothing.

**Known limits:**
- This value is separate from the check engine. `CheckEngine` and `CheckStore` still use only unlocks, unplugging and "I'm OK" (`ActivityTracker`). The usage log value is shown on the home screen only.
- The stored value can be up to about 12 h old. The home screen refreshes it each time it opens.
- Android keeps usage events for a limited time, so the lookback window should stay short.