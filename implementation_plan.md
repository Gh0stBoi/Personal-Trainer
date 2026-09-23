# AI Personal Trainer: Implementation Plan (Zero-Budget Edition)

> Written 23 Sep 2026. Free-tier limits and platform rules were checked on this date. They change often, so re-check anything marked ⚠️ before depending on it. Section 17 lists what was verified and what comes from general knowledge.

---

## 0. Summary

**What you are building:** an Android app that works as a personal trainer and accountability partner. Every day it plans your workout and meals, reminds you at the right time, asks check-in questions, tracks progress, and re-plans automatically when you skip something.

**The main decisions**

| Decision | Choice | Why |
|---|---|---|
| Platform | Native Android app (Kotlin + Jetpack Compose) | Reliable alarms, geofencing, Health Connect and on-device AI are all free on Android |
| Brain | Your own rules engine (plain code) | Free, offline, predictable, safe. Never breaks when an API changes |
| AI | Optional "voice" layer on top of the brain | Free AI tiers change constantly. The app must work with zero AI |
| Data | Local database on the phone (Room/SQLite) | No server means no hosting bill and full privacy |
| Reminders | Exact alarms + WorkManager + escalation ladder | Fires even when the app is closed |
| Distribution | Install the APK on your own phone by USB | $0. No Play Store fee |
| **Total cost** | **$0** | See section 12 for where money could sneak in |

**Assumption:** you have an Android phone. If you have an iPhone, read section 15 (Plan B).

---

## 1. Your wishes mapped to features

| You asked for | Feature that delivers it | Section |
|---|---|---|
| Workouts to do every day | Plan generator + Today screen | 6.2, 6.3 |
| Asks "did you do the workout / go to the gym / sleep enough?" | Check-in engine | 6.5 |
| Asks what you ate at each meal, and whether you cheated | Meal logging + off-plan handling | 6.5, 6.7 |
| Asks for weight and height | Scheduled body-metric prompts | 6.5, 6.9 |
| Alerts at gym time, and again if you're late | Reminder escalation ladder + gym geofence | 6.4 |
| If you skip, auto re-arrange and compensate | Rescheduler | 6.6 |
| "Make me do it" | Accountability system (be realistic, see 6.4) | 6.4 |
| Grow physique and health | Progression, nutrition, sleep and recovery, weekly review | 6.2 to 6.9 |
| Perfect, not just an API | Brain + Voice architecture | 2 |
| Zero money | Whole stack is free | 3, 12 |

---

## 2. Core idea: Brain and Voice

Most "AI trainer" projects are a chat box that calls an API. Those break the moment the API changes, and the AI forgets things, does bad maths and hallucinates. This design splits the job in two:

```
┌──────────────── VOICE (LLM, optional) ────────────────┐
│ Understands your messages ("had 2 eggs and toast"),   │
│ explains changes, motivates, writes weekly reviews    │
└──────────▲──────────────────────────┬─────────────────┘
   compact JSON summaries        proposals (validated JSON)
┌──────────┴──────────────────────────▼─────────────────┐
│ BRAIN (your code, always works, fully offline)        │
│ planner · rescheduler · nutrition math · readiness    │
│ reminder scheduler · progress tracker · safety limits │
└───────────────────────────────────────────────────────┘
```

**Rules that make it reliable**

1. The LLM never writes to the database directly. It proposes, and the brain validates.
2. The LLM never does maths (calories, loads, dates). Code does.
3. Safety limits live in code, not in prompts. The LLM cannot override them.
4. If every AI provider is down, templates still produce reminders, questions and summaries. The app never stops working.

**The LLM's jobs:** parse free-text meals into structured items, run the conversational check-in and chat, narrate the weekly review from real numbers, explain why the plan changed, suggest exercise swaps within constraints, and set the coach's tone.

**Not the LLM's jobs:** scheduling, calorie or load calculation, safety decisions, remembering your history (the database does that).

---

## 3. Zero-cost tech stack

| Layer | Choice | Cost | Notes |
|---|---|---|---|
| Language / UI | Kotlin + Jetpack Compose, Android Studio | Free | Best access to alarms, sensors and on-device AI. Flutter is a workable alternative (see 3.1) |
| Local DB | Room (SQLite). Optional SQLCipher for encryption | Free | All data stays on the phone |
| Background work | `AlarmManager` (exact), `WorkManager`, boot receiver | Free | Section 7 |
| Notifications | `NotificationCompat` with action buttons (Done / Snooze / Skip) and inline reply | Free | Answer check-ins without opening the app |
| Health data | Health Connect (steps, sleep, weight, exercise sessions, heart rate) | Free | Needs a source app writing to it (Samsung Health, Google Fit/Fitbit, Zepp, etc.) ⚠️ |
| Gym arrival detection | Play Services Geofencing API | Free | Needs background-location permission. Optional ⚠️ |
| Barcode scan | ML Kit barcode scanning (on-device) | Free | |
| Food data | Open Food Facts (barcodes, no key) + USDA FoodData Central (free key) + your own custom foods | Free | Your own recipes will be more accurate than any database |
| Exercise data | Start with your own list of 80 to 120 exercises. Optionally import an open dataset such as `free-exercise-db` | Free | Check the licence before bundling ⚠️ |
| Charts | Vico or MPAndroidChart (open source) | Free | |
| LLM tier A (on-device) | Gemma 4 via LiteRT-LM (Google AI Edge) | Free | Private and offline. Needs a fairly modern phone, see 3.2 |
| LLM tier B | Gemini API free tier (Flash / Flash-Lite via AI Studio, no card) | Free | Rate-limited. Free-tier prompts may be used to improve Google's products |
| LLM tier C | Groq free tier, OpenRouter `:free` models, Cloudflare Workers AI | Free | Backup providers |
| LLM tier D | Built-in templates (no AI) | Free | Always available |
| Backup | Android Auto Backup + manual encrypted JSON export | Free | |
| Version control / CI | Git + GitHub (free), GitHub Actions to build the APK | Free | |
| Coding help | Any free AI chat assistant | Free | See section 16 |

### 3.1 Why native Android and not Flutter or a web app

- A web app (PWA) cannot schedule reliable exact alarms and is a poor fit for a gym reminder that must fire when the phone is locked.
- Flutter works and gives you iOS later, but the important parts (alarms, geofence, Health Connect, LiteRT-LM) are all Android-native APIs, and you would be writing platform code anyway.
- If you already know Flutter well, use it. The architecture below still applies.

### 3.2 On-device LLM (optional, do this late)

- Google's LiteRT-LM runs Gemma models on Android with GPU acceleration and supports tool use (function calling).
- A published guide puts the quantized Gemma 4 E2B model at about 2.6 GB. It warns that the first inference after loading can take up to a minute on some devices, and that you should hold only one model instance at a time or you risk running out of memory.
- Treat this as a bonus. Test it on your phone in Phase 6. If it is too slow or your phone has little RAM, skip it. Tiers B, C and D still cover you.

### 3.3 Free cloud LLM reality check (Sept 2026)

Free tiers are useful but unstable. Numbers below are from third-party summaries, and they disagree with each other, so check the official dashboards.

| Provider | Reported free limits | Catch |
|---|---|---|
| Gemini (AI Studio) | Flash and Flash-Lite free, roughly 10 to 15 requests/min. Daily caps reported anywhere from 250 to 1,500 depending on model and date | Google says actual limits are per project, visible in AI Studio. Pro models reportedly moved behind billing. Free-tier content may be used to improve Google products ⚠️ |
| Groq | About 30 req/min, 1,000/day on one large open model, with a ~200K tokens/day ceiling | Limits change ⚠️ |
| OpenRouter `:free` | 20 req/min, 50 req/day (1,000/day only after buying $10 of credit) | Low daily cap. Backup only |
| Cloudflare Workers AI | 10,000 "Neurons"/day | Effective allowance depends on the model |
| GitHub Models | **Retired 30 Jul 2026** | Don't use it |

**Lesson:** one summary of this market notes that Google and Mistral stopped publishing free-tier numbers and GitHub Models was shut down. The advice is to treat free tiers as an evaluation budget and never let a critical path depend on one. That is why the brain works without any LLM.

**Call budget:** design for at most 10 to 15 LLM calls per day: about 4 for meal parsing, 1 to 3 for chat, 1 for the evening summary, plus the weekly review. Cache results by input hash. Never call the LLM from a background loop.

---

## 4. Architecture

```
app/
├─ ui/            Compose screens (Today, Log, Progress, Plan, Chat, Settings)
├─ data/          Room entities, DAOs, repositories, export/import
├─ engine/        THE BRAIN (pure Kotlin, no Android imports, fully unit-testable)
│   ├─ planner/         split selection, session builder, progression, deload
│   ├─ rescheduler/     move / merge / drop / re-entry logic
│   ├─ nutrition/       BMR/TDEE, macros, weekly calorie budget, adaptive TDEE
│   ├─ readiness/       sleep + soreness + energy score
│   ├─ progress/        trend weight, e1RM, PRs, adherence, plateau detection
│   └─ safety/          hard limits and red-flag rules
├─ scheduler/     AlarmScheduler, NotificationLadder, BootReceiver, Reconciler worker
├─ checkin/       question bank, flow rules, answer parsing
├─ ai/            LlmProvider interface, OnDevice/Gemini/Groq/OpenRouter/Template
│                 providers, ProviderRouter, PromptBuilder, JsonValidator, cache
├─ integrations/  HealthConnect, Geofence, Barcode, OpenFoodFacts
└─ settings/      API keys (encrypted), permission wizard, strictness level
```

**Event-driven core:** every user action or sensor input is written to an append-only `event` table (`SessionStarted`, `SessionSkipped`, `MealLogged`, `WeightLogged`, ...). The engine reads state and events and returns **Actions** (schedule this reminder, move this session). Benefits: easy debugging, undo, and replaying a week in a unit test.

**Inject a `Clock`.** All engine code takes the current time as a parameter so you can simulate weeks in milliseconds in tests.

---

## 5. Data model

| Table | Key fields |
|---|---|
| `user_profile` | sex, birth_year, height_cm, goal, experience, days_per_week, session_minutes, equipment[], injuries[], diet_prefs, wake_time, sleep_time, gym_lat/lng, strictness |
| `body_metric` | date, weight_kg, waist, chest, arm, thigh, hips, photo_uri, source |
| `exercise` | id, name, primary_muscle, secondary_muscles, equipment, pattern, is_compound, avoid_if[] |
| `program` | id, split_type, start_date, weeks, status |
| `planned_session` | id, program_id, date, original_date, type, status (planned / done / partial / skipped / moved), priority, target_minutes |
| `planned_set` | session_id, exercise_id, order, sets, rep_min, rep_max, target_load, target_rir |
| `set_log` | session_id, exercise_id, set_no, reps, load, rir, timestamp |
| `checkin` | timestamp, kind, question_key, answer, source (user / auto) |
| `meal_log` | date, slot, raw_text, items_json, kcal, protein, carbs, fat, confidence, off_plan |
| `sleep_log` | date, hours, quality_1_5, source |
| `reminder` | id, kind, fire_at, ref_id, state, escalation_level |
| `event` | ts, type, payload (append-only) |
| `llm_cache` | input_hash, response, created_at |

---

## 6. Feature specifications

### 6.1 Onboarding (about 10 minutes, once)

Collect: age, sex, height, weight, goal (fat loss / muscle gain / recomposition / general health), experience, days per week, session length, equipment, injuries and medical conditions, diet type and allergies, wake/sleep/work times, preferred workout window, gym location (map pin), and coach strictness (gentle / normal / strict). Optional: baseline photos and measurements.

Then run the **permission wizard**: notifications, exact alarms, battery "Unrestricted", Health Connect, location (only if you want gym auto-detect). Explain why for each one.

**Age gate:** if the user is under 18, do not generate calorie deficits. Focus on general fitness habits.

### 6.2 Plan generator (rules, not AI)

1. **Choose the split** from days per week: 2 to 3 days = full body, 4 = upper/lower, 5 to 6 = push/pull/legs (or PPL + upper/lower).
2. **Build sessions** from templates: 1 to 2 main compound lifts, 2 to 4 accessories, optional core or cardio. Filter by equipment and injuries. Rough time rule: each exercise of 3 sets takes about 6 to 8 minutes including rest, so a 60-minute session holds 6 to 8 exercises.
3. **Weekly volume:** about 10 to 20 hard sets per muscle group per week; beginners start near 8 to 10.
4. **Progression (double progression):** pick a rep range (e.g. 8 to 12). When every set hits the top of the range at the target effort, add the smallest load step (upper body +1 to 2.5 kg, lower body +2.5 to 5 kg) and drop back to the bottom of the range. If you miss the bottom of the range twice in a row, cut the load 5 to 10 percent.
5. **Deload** every 4 to 6 weeks, or when readiness is low for 3 sessions in a row: volume about -40 percent, load about -10 percent.
6. **Mesocycle:** after 4 to 8 weeks, review and regenerate the program using logged data.

### 6.3 Default daily rhythm (example: gym at 6 PM)

| When | What happens |
|---|---|
| Wake | Morning check-in: sleep hours, quality, energy, soreness, any pain (5 taps) |
| Wake + 30 min | "Today" card: workout, calorie and protein targets |
| Each meal slot + 30 min | "What did you eat?" (quick-pick, type, or barcode) |
| 90 min before gym | Pre-workout meal and water nudge |
| 45 min before | "Pack your bag, leave at 5:30" |
| Gym time | **Gym alert** with Start / Snooze 10 / Can't today |
| Gym time + 15 | "On your way?" (escalation, see 6.4) |
| Geofence enter | Auto-start session timer, no question needed |
| Planned end + 10 | "Time to wrap up" |
| After workout | Confirm done, effort 1 to 10, any pain, protein reminder |
| Dinner slot + 30 | Meal prompt |
| 9 PM | Day close: steps, anything off-plan, water, tomorrow's preview |
| Bedtime - 45 min | Wind-down reminder |

Cap total prompts at about 6 per day. Batch where possible. Skip any question that Health Connect already answered (sleep, steps, weight).

### 6.4 Reminder escalation and accountability

**Session state machine:** `PLANNED → NOTIFIED → STARTED → DONE | PARTIAL | SKIPPED | MOVED`

"Started" is detected by any of: you tap Start, geofence enter, or a Health Connect exercise session.

| Level | When | Action |
|---|---|---|
| L0 | T - 60 min | Gentle prep nudge |
| L1 | T | "Gym time" high-priority notification with buttons |
| L2 | T + 15 min, not started | "Going now? / Do the 20-minute version / Move it" |
| L3 | T + 45 min, not started | Louder alarm-style alert (if exact alarms and full-screen alerts are permitted) |
| L4 | End of window | "Skipped?" reason picker → hands over to the Rescheduler |
| Over-time | Planned end + 10 min | "Wrap up" |

**About "make me do it":** an app cannot physically force you. What it can do is make skipping harder and starting easier:

- **Minimum Viable Workout (MVW):** a 15 to 20 minute fallback session. Doing something protects the habit better than skipping.
- **Morning commitment:** "Will you train today at 6 PM? Yes / No." A "No" triggers rescheduling early, not a 6 PM surprise.
- **Streaks and weekly score:** count weeks where you hit your minimum, not perfect days, so one bad day doesn't kill motivation.
- **Buddy report:** a weekly summary you can share via WhatsApp or Telegram share intent.
- **Focus mode (manual):** set Android Digital Wellbeing focus mode for the gym window.
- **Strictness levels:** gentle, normal, strict change tone and the number of escalation steps. You choose.
- **Nudge budget:** maximum N alerts per day. Too many and you'll start ignoring them.

### 6.5 Check-in engine

Answer types: yes/no chips, 1 to 5 scale, number, free text (LLM parses it, brain validates it).

| Moment | Questions |
|---|---|
| Morning | Hours slept? Sleep quality 1 to 5? Energy? Soreness? Any pain or injury? |
| Weekly (chosen day, same conditions) | Weight. Waist. Optional photos |
| Monthly | Full measurements. Height only at onboarding and yearly (adults don't change) |
| Meals | What did you have for breakfast / lunch / dinner / snacks? On plan? Water? |
| Pre-gym | Eaten? Ready? |
| Post-gym | Done? Which parts? Log your loads? Effort 1 to 10? Any pain? |
| Evening | Steps? Anything off-plan today? (neutral wording, "just data, no guilt") Mood? |
| Weekly review | Confirm the week's summary and next week's schedule |

**Wording matters.** Use "off-plan", not "cheat". Shame makes people quit tracking.

### 6.6 Rescheduler (auto-compensation)

**Principles:** keep the weekly minimum, respect recovery, never punish. No double-day marathons and no extra cardio to "burn off" a cheat meal.

**Algorithm**

1. Classify the missed session: **key** (heavy compound day) or **minor** (accessories, cardio).
2. Find candidate slots in the next 1 to 3 days that pass all constraints: user availability, same muscle group at least 48 h apart, at most 3 to 4 consecutive training days, daily minutes cap, weekly volume floor.
3. **Slot found → MOVE.** Keep the split order (rolling split): delay the sequence instead of skipping a day in the cycle, and shift later sessions.
4. **No slot → MERGE.** Move key lifts into the next compatible session, drop accessories, and cap the added time at +15 min.
5. **Still no room → DROP** the minor parts. Keep at least the weekly minimum (default 2 sessions). Record "volume debt" and repay at most +10 percent next week.
6. **Gap handling** (days since last session):
   - 3 to 6 days: re-entry session at about 90 percent load, one set fewer.
   - 7 to 13 days: about 80 percent load.
   - 14 days or more: ramp-up week at about 70 percent load and two sets fewer, or regenerate the program.
7. **Special modes:** sick, travel, injury. Pause or swap to mobility and walking, then resume with the gap rules.

```kotlin
fun handleMissed(missed: Session, week: Week, ctx: Context): List<PlanChange> {
    val slots = ctx.availableDays(today, today.plusDays(3))
        .filter { canTrain(it, missed.muscles, week) }   // 48h rule, consecutive-day cap, minutes cap
    return when {
        slots.isNotEmpty() -> listOf(Move(missed, slots.best())) + shiftFollowing(week)
        canMerge(missed, week) -> listOf(MergeKeyLifts(missed, nextCompatible(week)), DropAccessories(missed))
        else -> listOf(Drop(missed), AddVolumeDebt(missed.hardSets, cap = 0.10))
    }
}
```

Treat all the numbers above as defaults to tune from your own data.

### 6.7 Nutrition

**Targets (computed by code)**

- BMR (Mifflin-St Jeor): `10·kg + 6.25·cm − 5·age + 5` (men) or `− 161` (women).
- TDEE = BMR × activity multiplier (1.2 sedentary, 1.375 light, 1.55 moderate, 1.725 very active).
- Goal adjustment: fat loss about -10 to -20 percent (cap the deficit near 500 kcal/day); muscle gain about +5 to +10 percent.
- Protein 1.6 to 2.2 g/kg (default about 1.8 to 2.0). Fat at least 0.6 to 0.8 g/kg. Carbs fill the rest. Water roughly 30 to 40 ml/kg.
- **Calorie floors** (default, configurable): about 1,500 kcal (men) and 1,200 kcal (women), unless a professional advises otherwise ⚠️.

**Adaptive TDEE:** after 2 to 3 weeks of data, estimate real expenditure = average intake − (change in trend weight in kg × 7,700 ÷ days). Nudge the target toward it.

**Logging methods (fastest first):** favorites and recents, your own saved recipes, barcode scan (Open Food Facts), typed text ("2 eggs, 2 toast, banana") parsed by the LLM then confirmed by you, and photo estimate (optional, low confidence). Always show a **confidence label** and let the user edit.

**Off-plan handling:** compute a weekly budget (7 × daily target). If you go over, spread the excess across the next 3 to 4 days, reducing each day by no more than 10 to 15 percent and never below the floor. On the last day of the week, just log it and move on.

### 6.8 Sleep and recovery (readiness score)

`readiness = 0.35·sleep_hours_score + 0.15·sleep_quality + 0.20·soreness + 0.15·energy_mood + 0.15·resting_HR/HRV (if available)`

| Score | Action |
|---|---|
| 70 to 100 | Train as planned |
| 40 to 69 | Cut volume about 20 percent and add one rep in reserve |
| Below 40 | Swap to mobility, walk or rest |
| Pain flag | Avoid the affected joint or exercise. If pain persists, suggest seeing a professional. This overrides the score |

Sleep target: 7 to 9 hours for adults. Send a bedtime wind-down reminder. Suggest a caffeine cut-off 6 to 8 hours before bed.

### 6.9 Progress tracking

- **Weight:** 7-day moving average and weekly rate. Warn if loss exceeds about 1 percent of body weight per week.
- **Strength:** estimated 1RM (Epley: `weight × (1 + reps/30)`), PR detection, weekly sets per muscle group.
- **Body:** measurements and photos (stored privately on the phone, same lighting and pose each time).
- **Adherence score:** sessions done vs planned, check-in rate, protein target hit days, average sleep.
- **Plateau detector (rules first):** weight flat for 3+ weeks against goal, or lifts stalled → suggested fix (adjust calories, deload, change rep range). The LLM only explains it.
- **Weekly review screen:** numbers come from the database, and the LLM only narrates them. It is never asked to compute anything.

### 6.10 AI layer

```kotlin
interface LlmProvider {
    val name: String
    suspend fun isAvailable(): Boolean
    suspend fun generate(req: LlmRequest): LlmResult   // text or schema-validated JSON
}
```

**ProviderRouter:** tries providers in your priority order (on-device → Gemini → Groq → OpenRouter → Template). On 429 or timeout it applies exponential backoff and a cooldown. A circuit breaker stops hammering a throttled provider. The last stop, `TemplateProvider`, never fails.

**Prompt structure:** (1) system prompt with persona and safety rules, (2) a compact context pack (today's plan, targets, 7-day summary, no name or exact location), (3) one task instruction, (4) a strict JSON schema. Validate the output. If invalid, retry once, then fall back.

**Closed set of intents** the LLM may return:

`log_meal`, `log_sleep`, `report_skip`, `request_reschedule`, `swap_exercise`, `ask_question`, `chat_only`

The brain checks each intent against rules and asks you to confirm anything that changes your plan.

**Privacy:** send the minimum. Free-tier cloud prompts may be used for model improvement, so keep sensitive details out, and prefer the on-device tier for anything personal. Provide a master "AI off" switch.

---

## 7. Making reminders reliable on Android

This is the part that decides whether the app is useful, so test it early (Phase 2).

1. **Notifications:** on Android 13+, request `POST_NOTIFICATIONS` at runtime.
2. **Exact alarms:** on Android 14 and higher, `SCHEDULE_EXACT_ALARM` is denied by default for new apps. Explain why, send the user to the "Alarms & reminders" settings screen with `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, check `canScheduleExactAlarms()` on every launch, and listen for the permission-state-change broadcast. `USE_EXACT_ALARM` is only for alarm and calendar apps and is reviewed on Google Play, so you don't need it for a sideloaded personal app.
3. **Doze mode:** use `setExactAndAllowWhileIdle()`.
4. **Reschedule everything** on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, timezone change and clock change.
5. **Reconciler worker:** runs every 15 minutes and on app open. It compares the `reminder` table with the alarms that actually exist and repairs any gap. Reminders must be self-healing.
6. **Manufacturer battery killers** (Xiaomi, Oppo, Vivo, Samsung, and others): guide the user to set battery to "Unrestricted" and enable autostart. Add a "Reminder health" screen showing which permissions are on and off. dontkillmyapp.com has per-brand instructions ⚠️.
7. **Notification channels:** separate channels for Gym Alerts (high), Check-ins (default) and Summaries (low), so the user can tune them.
8. **Alarm-style full-screen alerts** may need an extra special permission on recent Android versions ⚠️. Make them optional.
9. **Test matrix:** reboot, app swiped away, battery saver, Do Not Disturb, and forced Doze (`adb shell dumpsys deviceidle force-idle`).

---

## 8. Safety and health guardrails

- Not a medical device. Show a disclaimer in onboarding and ask about heart conditions, pregnancy, diabetes, injuries and medications. Any red flag → advise seeing a professional before starting.
- Hard caps in code: weight loss no more than about 1 percent of body weight per week, calorie floors, weekly volume increase at most about 10 percent, no punishment cardio, no extreme fasting advice.
- If the user repeatedly logs very low intake, expresses distress about food, or shows signs of disordered eating, the app softens its tone, stops pushing deficits, and suggests talking to a professional.
- Sharp or persistent pain → stop that exercise and suggest professional advice.
- The LLM cannot override any cap. Medical questions get general information plus "see a doctor".

---

## 9. Privacy and security

- Local-first, no account, no analytics, no trackers.
- API keys: the user pastes their own free key into Settings. It is stored with Android Keystore-backed encrypted preferences. **Never hard-code keys and never commit them to Git** (add to `.gitignore`).
- Optional database encryption (SQLCipher). Encrypted backups (password-protected JSON export).
- Photos live in app-private storage.
- Ask for the minimum permissions. Location is optional and only for gym geofencing.
- Add a "Delete all my data" button.

---

## 10. Roadmap

Assumes 8 to 10 hours per week while learning as you build. Total about 14 weeks, with a usable version by week 5.

| Phase | Weeks | Build | Done when |
|---|---|---|---|
| **0. Setup** | 1 | Install Android Studio, enable USB debugging, run "Hello World" on your phone, create the GitHub repo, hand-write a 4-week training plan as a reference | App launches on your phone |
| **1. Manual tracker** | 2 to 3 | Room DB, onboarding, exercise list, plan generator v1 (templates), Today screen, set logging, weight logging | You can run one full week manually inside the app |
| **2. Reminders + check-ins** | 4 to 5 | Permission wizard, AlarmScheduler, escalation ladder, notification actions, check-in flows, reconciler, boot receiver | 7 days of reminders arrive on time, including after reboot and with the app closed |
| **3. Rescheduler + nutrition** | 6 to 7 | Skip flow, move/merge/drop, re-entry rules, calorie and macro targets, meal logging (manual + favorites), off-plan budget | 20 scenario unit tests pass |
| **4. Progress + readiness** | 8 to 9 | Charts, moving-average weight, PRs, adherence score, weekly review, plateau rules, readiness score | Weekly review shows correct numbers from real data |
| **5. AI voice layer** | 10 to 11 | Provider interface, Gemini free + one backup, template fallback, meal parsing, chat, weekly narration, validators | Turning off Wi-Fi or removing the key leaves a fully working app |
| **6. Smart integrations** | 12 to 13 | Health Connect, geofence gym detection, barcode + Open Food Facts, optional on-device Gemma | Sleep and steps fill in automatically, and arriving at the gym starts the session |
| **7. Hardening** | 14+ | 30-day dogfooding, bug fixes, backup/export, battery audit, tune thresholds | You have used it daily for 30 days without turning reminders off |

**Rule:** do not start a phase until the previous "Done when" is true. Phases 1 and 2 alone already give you a working trainer.

---

## 11. Testing plan

- **Engine unit tests (JUnit):** planner, progression, rescheduler, nutrition formulas, readiness. Use a table of scenarios, for example: miss Monday with a free Tuesday; miss two days in a row; miss all week; 10-day illness; cheat on Saturday; skip on the last day of the week.
- **Time travel:** with the injected `Clock`, simulate 12 weeks and assert that no muscle group is trained within 48 h, weekly minimums are met, and nothing goes below the calorie floor.
- **Database:** Room instrumented tests.
- **Reminders:** manual test matrix from section 7 on a real phone.
- **LLM:** run 50 sample meal texts and check that the JSON is valid at least 95 percent of the time. Test the fallback by using a wrong key and by turning the network off.
- **Dogfooding:** keep an "annoyances" list and count how many notifications you dismiss without acting.

---

## 12. Cost table

| Item | Cost | Notes |
|---|---|---|
| Android Studio, Kotlin, Room, WorkManager, ML Kit, Health Connect | $0 | |
| Test device | $0 | Your own phone, or the emulator for non-alarm work |
| Distribution | $0 | Install the APK by USB/ADB. Google now offers a free "limited distribution" developer account (up to 20 devices, no fee, no government ID) if you ever want to share it |
| LLM | $0 | Free tiers + on-device + templates |
| Hosting / servers | $0 | Not needed |
| Source control / CI | $0 | GitHub free |

**Where money could sneak in (avoid these):** Google Play Console (one-time $25), Apple Developer Program (about $99/year), any VPS or cloud server, any paid LLM plan, and paid food or exercise databases.

---

## 13. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Free LLM tier changes or disappears (it already happened with GitHub Models) | Provider interface, router with fallback, template tier, brain works without AI |
| OS or manufacturer kills reminders | Permission wizard, reconciler, "Reminder health" screen, per-brand guidance |
| Notification fatigue | Daily nudge cap, batching, strictness setting, auto-silence after repeated dismissals |
| Scope creep | Phased roadmap. Phases 1 and 2 are the real MVP |
| Inaccurate calorie estimates | Confidence labels, custom foods, trend-based adaptive targets |
| Unsafe advice | Hard caps in code, disclaimers, red-flag rules |
| Data loss | Auto Backup + manual export every week |
| Losing motivation to build it | Dogfood from Phase 2. Use it on yourself early |
| Android sideloading policy | Google's developer verification enforcement begins 30 Sep 2026, but only in Brazil, Indonesia, Singapore and Thailand and only for certain app stores. Installing via ADB and an "advanced flow" remain available, and broader rollout is planned for 2027. Register a free limited-distribution account later if needed |

---

## 14. Suggested project layout in Git

```
ai-trainer/
├─ PLAN.md                  (this file)
├─ app/                     Android app (see section 4)
├─ engine-tests/            scenario tables for planner and rescheduler
├─ docs/
│   ├─ question-bank.md
│   ├─ notification-copy.md
│   └─ decisions.md         (short "why we chose X" notes)
└─ .gitignore               (keys, local.properties, build outputs)
```

---

## 15. Plan B: if you have an iPhone

Free Apple ID builds expire after 7 days and anything longer needs the paid developer program, so a native iOS app is not a zero-cost option. Alternative:

- Run the same brain as a **Telegram bot** written in TypeScript.
- Host it on Cloudflare Workers (cron triggers for reminders, free storage) ⚠️ verify current free limits.
- Check-ins become chat messages with inline buttons; reminders arrive as normal push notifications.
- Trade-offs: a chat notification is easier to ignore than an alarm, and it needs an internet connection. You lose geofence and alarm-style alerts. The engine design (sections 2 to 6) is identical.

---

## 16. Building it with free AI coding help

1. Work in small tasks: one function or screen at a time.
2. For engine code, ask for **tests first**, then the implementation.
3. Paste the relevant section of this plan as context every time.
4. Ask for code that compiles with the exact versions in your `build.gradle`.
5. Commit after every working step.

Example prompts:

- "Here is section 6.6 of my plan. Write Kotlin data classes for `Session`, `Week` and `PlanChange`, then 10 JUnit tests for the rescheduler covering [scenario list]. Don't write the implementation yet."
- "Write an `AlarmScheduler` that uses `setExactAndAllowWhileIdle`, checks `canScheduleExactAlarms()`, and re-registers alarms from the `reminder` table on boot."

---

## 17. Sources and what was verified

**Checked on 23 Sep 2026**

- Android exact alarms: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms and https://developer.android.com/develop/background-work/services/alarms
- Developer verification: https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html and https://developer.android.com/developer-verification/guides/faq
- On-device LLM: https://ai.google.dev/edge/litert-lm and https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/
- Gemini limits: https://ai.google.dev/gemini-api/docs/rate-limits and https://pecollective.com/tools/gemini-free-tier-guide/
- Free LLM APIs: https://continuumcode.ai/guides/free-llm-api/ , https://www.kdnuggets.com/5-free-llm-api-providers-you-can-use-in-2026 and https://openrouter.ai/blog/tutorials/free-llm-apis-compared/

**From general knowledge, not re-verified today (check before relying on them):** Health Connect data types and source-app requirement, Geofencing API and background-location rules, ML Kit barcode scanning being free, Open Food Facts and USDA API terms, `free-exercise-db` licence, Cloudflare Workers free limits, full-screen intent permission rules, Apple's developer program price, and the nutrition and training numbers (widely used guidelines, but they are defaults to tune, not medical advice).

---

## Appendix A: Sample notification copy

| Moment | Gentle | Strict |
|---|---|---|
| T - 60 | "Leg day at 6. Want to eat something light now?" | "Leg day in 1 hour. Eat and pack your bag." |
| T | "Gym time 💪 Ready when you are." | "Gym time. Start now." |
| T + 15 | "Still at home? A 20-minute version counts too." | "You're late. Go now or pick the 20-minute version." |
| T + 45 | "Want me to move today's session? Tap to reschedule." | "Last call. Train, shorten, or tell me why you're skipping." |
| Evening | "Anything off-plan today? Just data, no judgment." | "Log your meals. Off-plan counts. Log it honestly." |

## Appendix B: Sample LLM system prompt

```
You are a supportive, honest fitness coach inside a personal app.
Rules:
- Never diagnose or give medical advice. For pain, illness or medical questions,
  give general info and suggest seeing a professional.
- Never suggest calories below the user's floor or any extreme diet or fasting.
- Never shame the user. Use neutral wording ("off-plan", not "cheat").
- You do not calculate calories, loads or dates. Use the numbers provided.
- When asked for JSON, return ONLY valid JSON matching the schema. No prose.
- Notification text: max 25 words. Chat replies: max 80 words unless asked.
```

## Appendix C: Sample meal-parsing schema

```json
{
  "intent": "log_meal",
  "slot": "breakfast",
  "items": [
    { "name": "egg", "qty": 2, "unit": "piece", "kcal": 140, "protein_g": 12, "carbs_g": 1, "fat_g": 10, "confidence": 0.8 },
    { "name": "toast", "qty": 2, "unit": "slice", "kcal": 160, "protein_g": 6, "carbs_g": 30, "fat_g": 2, "confidence": 0.7 }
  ],
  "needs_confirmation": true
}
```

The brain checks the numbers against known foods, flags anything implausible, and shows the result for you to confirm or edit before saving.
