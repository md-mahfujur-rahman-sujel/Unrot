# Unrot — Architecture Document

## Overview

Unrot is a digital-wellbeing Android app that limits daily time on social media apps. When a limit is reached, a full-screen block appears; users must solve arithmetic problems to earn extra minutes.

**Stack:** Kotlin, Jetpack Compose, Room, Hilt, WorkManager, UsageStatsManager

**Package:** `com.unrot.app`

---

## Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  Onboarding │ Home │ AppPicker │ Stats │ Settings       │
│  BlockActivity (math unlock overlay)                    │
├─────────────────────────────────────────────────────────┤
│                   ViewModels                            │
│  OnboardingVM │ HomeVM │ AppPickerVM │ BlockVM │ ...   │
├─────────────────────────────────────────────────────────┤
│                   Domain Layer                          │
│  Models │ GenerateMathProblems │ CalculateUnlockDiff   │
├─────────────────────────────────────────────────────────┤
│                   Data Layer                            │
│  LimitRepository │ Room (UnrotDatabase)                 │
├─────────────────────────────────────────────────────────┤
│                   System Layer                          │
│  UsageMonitorService │ BootReceiver │ DailyResetWorker  │
└─────────────────────────────────────────────────────────┘
```

---

## Core Flow

### 1. Monitoring Loop (`UsageMonitorService`)

```
Every 2 seconds:
  1. Query UsageStatsManager for foreground app
  2. If app is limited → accumulate session time
  3. If budget exhausted → launch BlockActivity
  4. Update persistent notification with remaining time
```

### 2. Block & Unlock Flow

```
User opens limited app (budget = 0)
  → BlockActivity launched (singleTask, excludeFromRecents)
  → User taps "Solve to add time"
  → CalculateUnlockDifficulty(unlockCountToday)
  → GenerateMathProblems(count, difficulty)
  → User solves N problems (wrong answers don't fail session)
  → grantExtraTime(+5 min) → dismiss block
```

### 3. Difficulty Scaling

| Unlock # today | Problems required |
|---|---|
| 1st | 5 (base) |
| 2nd | 8 |
| 3rd | 12 |
| 4th+ | 12 + 4 per extra |

---

## Database Schema (Room)

### `limited_apps`
| Column | Type | Description |
|---|---|---|
| packageName | PK String | App package |
| appName | String | Display name |
| dailyBudgetMinutes | Int | Daily limit |
| usedTodayMs | Long | Time used today |
| extraTimeMs | Long | Bonus time from unlocks |
| unlockCountToday | Int | Unlock attempts today |
| lastResetDate | String | ISO date of last reset |
| isEnabled | Boolean | Active limit |

### `usage_logs`
Daily snapshots for weekly stats.

### `unlock_events`
Each math-unlock attempt (problems required/solved, minutes granted).

### `app_settings`
Singleton row: onboarding state, math difficulty, protection toggle.

---

## Permissions

| Permission | Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | Detect foreground app |
| `SYSTEM_ALERT_WINDOW` | Reserved for V2 overlay; BlockActivity used in MVP |
| `FOREGROUND_SERVICE` | Keep monitor alive |
| `RECEIVE_BOOT_COMPLETED` | Restart after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | OEM survival guide |

---

## File Map

```
com/unrot/app/
├── UnrotApplication.kt          Hilt + WorkManager config
├── MainActivity.kt              Entry point, nav routing
├── di/AppModule.kt              Room + DAO providers
├── data/
│   ├── local/
│   │   ├── UnrotDatabase.kt
│   │   ├── entity/              LimitedApp, UsageLog, UnlockEvent, Settings
│   │   └── dao/
│   └── repository/LimitRepository.kt
├── domain/
│   ├── model/                   LimitedApp, MathProblem, etc.
│   └── usecase/                 GenerateMathProblems, CalculateUnlockDifficulty
├── service/UsageMonitorService.kt
├── receiver/BootReceiver.kt
├── worker/DailyResetWorker.kt
├── util/                        DateUtils, PermissionHelper, SocialApps
└── ui/
    ├── navigation/UnrotNavGraph.kt
    ├── onboarding/
    ├── apps/
    ├── home/
    ├── block/BlockActivity.kt   Lock screen + math
    ├── stats/
    ├── settings/
    └── theme/
```

---

## Screens

| Screen | Route | Purpose |
|---|---|---|
| Onboarding | `onboarding` | Permission checklist |
| Home | `home` | Today's remaining time per app |
| App Picker | `apps` | Select apps + set budgets |
| Stats | `stats` | Usage + unlock request counts |
| Settings | `settings` | Difficulty, pause protection |
| Block | Activity | Full-screen lock + math challenge |

---

## V1 Roadmap (not yet implemented)

- [x] Schedule-based hard blocks (quiet hours) — implemented in AppPickerScreen + LimitedApp.isBlockedBySchedule
- [ ] BlockOverlayService with `TYPE_APPLICATION_OVERLAY`
- [ ] Accessibility Service strict mode
- [ ] OEM-specific battery optimization guides
- [ ] Weekly trend comparison
- [ ] Freemium / premium tier

---

## Known Limitations

1. **Not unbypassable** — users can revoke permissions or uninstall
2. **1–3 sec detection lag** — UsageStatsManager polling interval
3. **OEM battery killers** — may stop service on Xiaomi/Huawei without whitelisting
4. **BlockActivity vs overlay** — MVP uses Activity; user may briefly see blocked app

---

## Build & Run

1. Open project in Android Studio
2. Sync Gradle
3. Run on device/emulator (API 26+)
4. Grant Usage Access + Overlay permissions during onboarding
5. Pick apps, set budgets, test by setting a 1-minute limit
