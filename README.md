# AI Personal Trainer

A zero-cost, privacy-first Android personal trainer app built with Kotlin + Jetpack Compose.

## Architecture

This app is built on a **Brain + Voice** architecture:

- **Brain** — pure Kotlin engine (no Android imports) that handles all logic: planning, scheduling, nutrition math, readiness, rescheduling, and safety. Always works, fully offline.
- **Voice** — optional LLM layer for natural language parsing and coaching. Falls back to templates if unavailable.

## Features (Phase Roadmap)

| Phase | Status | Features |
|-------|--------|---------|
| 0 | ✅ | Project setup, data model, engine foundations |
| 1 | 🚧 | Manual tracker, plan generator, Today screen |
| 2 | ⏳ | Reliable reminders, check-ins, boot reconciler |
| 3 | ⏳ | Rescheduler, nutrition tracking |
| 4 | ⏳ | Progress charts, weekly review |
| 5 | ⏳ | AI voice layer (Gemini/Groq/Template fallback) |
| 6 | ⏳ | Health Connect, geofencing, barcode |

## Tech Stack

- **UI**: Jetpack Compose + Material3
- **Database**: Room (SQLite)
- **DI**: Hilt
- **Background**: AlarmManager (exact) + WorkManager
- **AI**: Gemini free tier → Groq → Template fallback
- **Charts**: Vico
- **Total cost**: $0

## Building

1. Open in Android Studio Hedgehog or newer
2. Enable USB Debugging on your Android phone
3. Run `app` configuration — installs directly via ADB

### Optional: Gemini AI
1. Get a free API key at [ai.google.dev](https://ai.google.dev)
2. Paste it in **Settings → AI Provider**
3. The app works fully without it (template responses)

## Running Tests

```bash
./gradlew test                 # Unit tests (JVM, fast)
./gradlew connectedAndroidTest # Instrumented tests (needs a device)
```

## Privacy

- Local-first: no account, no analytics, no trackers
- All data stays on your phone
- API keys stored in Android Keystore-backed encrypted preferences
- Photos in app-private storage
- One-tap "Delete all my data" in Settings

## Safety

This is a fitness tool, not a medical device. Hard-coded safety limits in the engine:
- Calorie floors: 1,500 kcal (men), 1,200 kcal (women)
- Max weekly weight loss: 1% of body weight
- Max weekly volume increase: 10%
- The LLM **cannot** override any of these limits

See `implementation_plan.md` for the complete technical specification.
