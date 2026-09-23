package com.personaltrainer.ui.today

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.personaltrainer.data.entities.*
import com.personaltrainer.engine.readiness.ReadinessRecommendation
import com.personaltrainer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Today screen — the app's daily dashboard.
 *
 * Shows:
 *  - Date + greeting
 *  - Readiness score ring
 *  - Today's workout card(s)
 *  - Macro ring progress
 *  - Quick meal log
 *  - Upcoming reminders / check-ins
 */
@Composable
fun TodayScreen(
    navController: NavController,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val sessions by viewModel.todaySessions.collectAsStateWithLifecycle()
    val macroTargets by viewModel.macroTargets.collectAsStateWithLifecycle()
    val nutritionTotals by viewModel.todayNutritionTotals.collectAsStateWithLifecycle()
    val readiness by viewModel.readinessResult.collectAsStateWithLifecycle()

    val todayLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A1A35), Background))
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column {
                Text(
                    todayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    greeting(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Readiness score ────────────────────────────────────────────
            readiness?.let { r ->
                ReadinessCard(score = r.score, recommendation = r.recommendation)
            }

            // ── Workout cards ──────────────────────────────────────────────
            if (sessions.isEmpty()) {
                RestDayCard()
            } else {
                sessions.forEach { session ->
                    WorkoutSessionCard(
                        session = session,
                        onStart = { viewModel.markSessionStarted(session.id) },
                        onDone = { viewModel.markSessionDone(session.id) },
                        onSkip = { viewModel.markSessionSkipped(session.id, "user_skipped") }
                    )
                }
            }

            // ── Macro progress ─────────────────────────────────────────────
            macroTargets?.let { targets ->
                MacroProgressCard(totals = nutritionTotals, targets = targets)
            }

            // ── Quick check-in prompt ──────────────────────────────────────
            MorningCheckinPromptCard()
        }
    }
}

// ─── Readiness Card ──────────────────────────────────────────────────────────

@Composable
private fun ReadinessCard(score: Int, recommendation: ReadinessRecommendation) {
    val color = when {
        score >= 70 -> Secondary
        score >= 40 -> Color(0xFFFFB347)   // amber
        else -> Tertiary
    }
    val label = when (recommendation) {
        ReadinessRecommendation.TRAIN_AS_PLANNED -> "Train as planned"
        ReadinessRecommendation.REDUCED_VOLUME -> "Light session recommended"
        ReadinessRecommendation.REST -> "Rest or mobility today"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score ring
            ScoreRing(score = score, color = color, size = 72.dp)
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Readiness", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Text(
                    "$score / 100",
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, color: Color, size: Dp) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "readiness_ring"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
            // Background track
            drawArc(
                color = OutlineVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            "$score",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Rest Day Card ───────────────────────────────────────────────────────────

@Composable
private fun RestDayCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("😴", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("Rest Day", style = MaterialTheme.typography.titleLarge, color = OnSurface, fontWeight = FontWeight.Bold)
            Text("Recovery is where the growth happens.", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
    }
}

// ─── Workout Session Card ─────────────────────────────────────────────────────

@Composable
private fun WorkoutSessionCard(
    session: PlannedSession,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    val (emoji, label) = when (session.type) {
        SessionType.PUSH -> "🫷" to "Push Day"
        SessionType.PULL -> "🫸" to "Pull Day"
        SessionType.LEGS -> "🦵" to "Leg Day"
        SessionType.UPPER -> "💪" to "Upper Body"
        SessionType.LOWER -> "🦵" to "Lower Body"
        SessionType.FULL_BODY -> "🏋️" to "Full Body"
        SessionType.CARDIO -> "🏃" to "Cardio"
        SessionType.MOBILITY -> "🧘" to "Mobility"
        SessionType.DELOAD -> "🔄" to "Deload Week"
        SessionType.RE_ENTRY -> "🔁" to "Re-Entry Session"
        SessionType.MINIMUM_VIABLE -> "⚡" to "Express Workout"
    }

    val statusColor = when (session.status) {
        SessionStatus.DONE -> Secondary
        SessionStatus.STARTED -> Primary
        SessionStatus.SKIPPED -> Tertiary
        else -> OnSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleLarge, color = OnSurface, fontWeight = FontWeight.Bold)
                    Text("${session.targetMinutes} min", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                // Status chip
                StatusChip(session.status)
            }

            if (session.status == SessionStatus.PLANNED || session.status == SessionStatus.NOTIFIED) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Start")
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }
                }
            }

            if (session.status == SessionStatus.STARTED) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Complete")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: SessionStatus) {
    val (label, color) = when (status) {
        SessionStatus.PLANNED -> "Planned" to OnSurfaceVariant
        SessionStatus.NOTIFIED -> "Upcoming" to Primary
        SessionStatus.STARTED -> "Active" to Primary
        SessionStatus.DONE -> "Done ✓" to Secondary
        SessionStatus.PARTIAL -> "Partial" to Color(0xFFFFB347)
        SessionStatus.SKIPPED -> "Skipped" to Tertiary
        SessionStatus.MOVED -> "Moved" to OnSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ─── Macro Progress Card ──────────────────────────────────────────────────────

@Composable
private fun MacroProgressCard(totals: NutritionTotals, targets: com.personaltrainer.engine.nutrition.MacroTargets) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Nutrition Today", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            // Calories
            MacroBar(
                label = "Calories",
                current = totals.kcal.toInt(),
                target = targets.kcal.toFloat(),
                unit = "kcal",
                color = Primary
            )
            Spacer(Modifier.height(10.dp))
            MacroBar(
                label = "Protein",
                current = totals.proteinG.toInt(),
                target = targets.proteinG.toFloat(),
                unit = "g",
                color = Secondary
            )
            Spacer(Modifier.height(10.dp))
            MacroBar(
                label = "Carbs",
                current = totals.carbsG.toInt(),
                target = targets.carbsG.toFloat(),
                unit = "g",
                color = Color(0xFFFFB347)
            )
            Spacer(Modifier.height(10.dp))
            MacroBar(
                label = "Fat",
                current = totals.fatG.toInt(),
                target = targets.fatG.toFloat(),
                unit = "g",
                color = Tertiary
            )
        }
    }
}

@Composable
private fun MacroBar(label: String, current: Int, target: Float, unit: String, color: Color) {
    val progress by animateFloatAsState(
        targetValue = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(800),
        label = "macro_$label"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text("$current / ${target.toInt()} $unit", style = MaterialTheme.typography.bodySmall, color = OnSurface)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = OutlineVariant
        )
    }
}

// ─── Morning Check-in Prompt ─────────────────────────────────────────────────

@Composable
private fun MorningCheckinPromptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = PrimaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Morning check-in", style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text("Log sleep, energy & soreness", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

// ─── Helper ──────────────────────────────────────────────────────────────────

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning 🌅"
        hour < 17 -> "Good afternoon ☀️"
        else -> "Good evening 🌙"
    }
}
