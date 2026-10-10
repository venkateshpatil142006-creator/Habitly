# Habitly — offline-first Android habit tracker

Native Android app (Kotlin, Jetpack Compose, Room, AlarmManager). No account, no network, no paid service.

## Features
- Create / edit / delete habits: name, type (yes/no, countable with custom unit, timed in minutes), schedule (daily, specific weekdays, N times per week), target, optional reminder time
- Today screen with quick logging (checkbox, +/- buttons, or type an exact value)
- Correct any previous day from the habit's calendar view (tap a day)
- Current + longest streak (schedule-aware), completion rate, total completions, 6-week calendar
- Per-habit reminders that survive app restarts and phone reboots
- All data in a local Room (SQLite) database
- **Bonus:** "Mark done" action directly in the notification, light/dark theme (follows system, dynamic colour on Android 12+)

## Screenshots
| Today | Habits | Detail + calendar | New habit |
|---|---|---|---|
| ![Today](screenshots/today.png) | ![Habits](screenshots/habits.png) | ![Detail](screenshots/detail.png) | ![Edit](screenshots/edit.png) |

## Setup / build
1. Install Android Studio (Koala or newer) with JDK 17 (bundled).
2. `git clone <repo-url>` then **File → Open** the `Habitly` folder. Let Gradle sync finish.
3. If the Gradle download times out on your network, open `gradle/wrapper/gradle-wrapper.properties` and use the official URL `https\://services.gradle.org/distributions/gradle-8.9-bin.zip` (or any Gradle mirror reachable from your network).
4. Run on an emulator / device (Android 8.0+, API 26+) with the green ▶ button, or build an APK: **Build → Build Bundle(s)/APK(s) → Build APK(s)**. Output: `app/build/outputs/apk/debug/app-debug.apk`.
5. Run the tests: `./gradlew test` (or right-click `app/src/test` → Run Tests). If `./gradlew` is missing, run `gradle wrapper` once or just use Android Studio.

## Architecture
```
domain/    pure Kotlin: Habit, HabitLog, StreakCalculator, ReminderPlanner, DateUtils  (no Android imports -> unit tested)
data/      Room entities, DAOs, database, HabitRepository (maps entity <-> domain)
reminder/  ReminderScheduler (AlarmManager), ReminderReceiver, BootReceiver, Notifier
ui/        Compose screens + HabitViewModel (single source of UI state)
```
UI never contains streak logic; it only calls `StreakCalculator`. 

## Answers
**Why this stack?** Reminders that fire on time and survive reboot are most reliable with native Android APIs (AlarmManager + BroadcastReceiver). Compose gives a clean UI with little code, Room gives a typed offline database. Pure-Kotlin domain layer keeps the tricky logic testable on the JVM without an emulator.

**How are streaks computed?**
- Daily / weekday habits: walk backwards from today over *scheduled* days. Completed day → +1. Unscheduled day → skipped. A scheduled past day not completed → streak ends. Today still incomplete does *not* break it (the day isn't over).
- Countable/timed: a day counts only if `value >= target` (partial progress doesn't count).
- N times per week: weeks run Monday–Sunday; a week is successful if it has ≥ N completed days. Streak = consecutive successful weeks (shown in weeks). The current week is "in progress" and never breaks the streak until it ends.
- Because streaks are recomputed from the logs every time, editing a past day automatically gives the right result.

**How do reminders work? What after restart?** Each habit with a reminder has one exact one-shot alarm (`setExactAndAllowWhileIdle`) for its next valid day/time (`ReminderPlanner`). When it fires, `ReminderReceiver` first schedules the next occurrence, then shows a notification (skipped if the habit is already done today). `BootReceiver` listens for `BOOT_COMPLETED`, app update, time change and time-zone change and re-arms all alarms. The app also re-arms on every launch.

**Dates & time zones?** Logs are keyed by the device's local calendar date (`LocalDate`, stored as epoch-day). "Today" comes from the system clock/zone and the UI re-checks it every 20 s, so it rolls over at midnight. Reminders use `ZonedDateTime` in the current zone and are rescheduled when the zone changes. Limitation: if you travel across time zones, old logs are not shifted.

**Assumptions / limitations**
- Logging a day *before* a habit's start date moves the habit's start date back to that day (so you can backfill history); days between are then counted as missed.
- Habit type can't be changed after creation (avoids mixing yes/no and numeric logs).
- On Android 13+ the app uses `USE_EXACT_ALARM` (auto-granted). On Android 12 the user may need to allow exact alarms; otherwise it falls back to inexact alarms (can be a few minutes late). Some OEMs (Xiaomi, Oppo…) kill background apps — disable battery optimisation for reliable reminders.
- Timed habits log minutes manually (no built-in countdown timer).

**With more time:** Pomodoro countdown timer, home-screen widget, export/import (JSON), instrumented UI tests, per-habit charts, and a reading-habit extension (store book/author, show offline author/genre suggestions from a bundled dataset, and cache data for new authors fetched once online for later offline use).

## Tests
`app/src/test/.../StreakCalculatorTest.kt` and `ReminderPlannerTest.kt` — 26 tests covering: the task's example, missed day breaking a streak, unscheduled days, partial progress, editing a past day, N-times-per-week, week boundaries, logs near midnight, reminder planning.


## AI Usage Disclosure

Claude was used as an AI-assisted development tool for conceptual clarification, coding guidance, and assistance with specific technical challenges during the development process. Its suggestions were evaluated and incorporated where appropriate, alongside independent analysis, iterative testing, debugging, and refinement of the implementation. The tool served as a supplementary resource to support development and problem-solving.
