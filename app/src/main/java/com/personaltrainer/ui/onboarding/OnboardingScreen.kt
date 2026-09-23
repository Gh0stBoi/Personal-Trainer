package com.personaltrainer.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltrainer.data.entities.*
import com.personaltrainer.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Multi-page onboarding flow (~10 minutes, done once).
 *
 * Pages:
 *  0. Welcome + disclaimer
 *  1. Demographics (age, sex, height, weight)
 *  2. Goal + experience
 *  3. Schedule (days/week, session length, workout time)
 *  4. Equipment
 *  5. Strictness
 *  6. Summary + save
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val totalPages = 7
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Background, Color(0xFF12122A))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress indicator
            OnboardingProgressBar(
                currentPage = pagerState.currentPage,
                totalPages = totalPages,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false   // Navigation via buttons only
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        onAcceptDisclaimer = { viewModel.hasAcceptedDisclaimer = it }
                    )
                    1 -> DemographicsPage(viewModel)
                    2 -> GoalPage(viewModel)
                    3 -> SchedulePage(viewModel)
                    4 -> EquipmentPage(viewModel)
                    5 -> StrictnessPage(viewModel)
                    6 -> SummaryPage(viewModel)
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < totalPages - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                viewModel.saveProfile(onComplete)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (pagerState.currentPage == totalPages - 1) "Start Training!" else "Continue")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (pagerState.currentPage == totalPages - 1) Icons.Filled.FitnessCenter
                        else Icons.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressBar(currentPage: Int, totalPages: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalPages) { i ->
            val isActive = i <= currentPage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) Primary else OutlineVariant)
            )
        }
    }
}

// ─── Page 0: Welcome ─────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onAcceptDisclaimer: (Boolean) -> Unit) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💪", fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "Your Personal Trainer",
            style = MaterialTheme.typography.headlineLarge,
            color = OnBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI-powered workouts, nutrition tracking, and smart reminders — all offline, all free.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // Health disclaimer card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = Tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Health Disclaimer", style = MaterialTheme.typography.titleMedium, color = Tertiary)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "This app is for general fitness guidance only — not a medical device. " +
                    "If you have any heart conditions, pregnancy, diabetes, injuries, or are " +
                    "on medications, consult a healthcare professional before starting.\n\n" +
                    "All training and nutrition values are defaults to tune from your own experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        accepted = !accepted
                        onAcceptDisclaimer(accepted)
                    }
                ) {
                    Checkbox(checked = accepted, onCheckedChange = {
                        accepted = it
                        onAcceptDisclaimer(it)
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("I understand and accept", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                }
            }
        }
    }
}

// ─── Page 1: Demographics ────────────────────────────────────────────────────

@Composable
private fun DemographicsPage(vm: OnboardingViewModel) {
    var sex by remember { mutableStateOf(vm.sex) }
    var birthYear by remember { mutableStateOf(vm.birthYear.toString()) }
    var heightCm by remember { mutableStateOf(vm.heightCm.toInt().toString()) }
    var weightKg by remember { mutableStateOf(vm.weightKg.toInt().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(emoji = "👤", title = "About you", subtitle = "Used to calculate accurate calorie and macro targets")

        // Sex selector
        Text("Biological sex", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Sex.entries.forEach { s ->
                FilterChip(
                    selected = sex == s,
                    onClick = { sex = s; vm.sex = s },
                    label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Birth year
        OutlinedTextField(
            value = birthYear,
            onValueChange = {
                birthYear = it
                it.toIntOrNull()?.let { y -> vm.birthYear = y }
            },
            label = { Text("Birth year") },
            supportingText = { Text("e.g. 1995") },
            modifier = Modifier.fillMaxWidth()
        )

        // Height
        OutlinedTextField(
            value = heightCm,
            onValueChange = {
                heightCm = it
                it.toFloatOrNull()?.let { h -> vm.heightCm = h }
            },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Weight
        OutlinedTextField(
            value = weightKg,
            onValueChange = {
                weightKg = it
                it.toFloatOrNull()?.let { w -> vm.weightKg = w }
            },
            label = { Text("Current weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Page 2: Goal ────────────────────────────────────────────────────────────

@Composable
private fun GoalPage(vm: OnboardingViewModel) {
    var goal by remember { mutableStateOf(vm.goal) }
    var experience by remember { mutableStateOf(vm.experience) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(emoji = "🎯", title = "Your goal", subtitle = "This shapes your entire training and nutrition plan")

        Text("Primary goal", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { g ->
                val (icon, desc) = when (g) {
                    Goal.FAT_LOSS -> "🔥" to "Lose body fat while preserving muscle"
                    Goal.MUSCLE_GAIN -> "💪" to "Build muscle mass and strength"
                    Goal.RECOMPOSITION -> "⚖️" to "Lose fat and gain muscle simultaneously"
                    Goal.GENERAL_HEALTH -> "❤️" to "Improve fitness, energy, and wellbeing"
                }
                GoalCard(
                    emoji = icon,
                    title = g.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    description = desc,
                    selected = goal == g,
                    onClick = { goal = g; vm.goal = g }
                )
            }
        }

        Text("Training experience", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExperienceLevel.entries.forEach { e ->
                val desc = when (e) {
                    ExperienceLevel.BEGINNER -> "Less than 1 year of consistent training"
                    ExperienceLevel.INTERMEDIATE -> "1–3 years of consistent training"
                    ExperienceLevel.ADVANCED -> "3+ years, familiar with progressive overload"
                }
                GoalCard(
                    emoji = when (e) { ExperienceLevel.BEGINNER -> "🌱"; ExperienceLevel.INTERMEDIATE -> "🌿"; else -> "🌳" },
                    title = e.name.lowercase().replaceFirstChar { it.uppercase() },
                    description = desc,
                    selected = experience == e,
                    onClick = { experience = e; vm.experience = e }
                )
            }
        }
    }
}

@Composable
private fun GoalCard(emoji: String, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryContainer else SurfaceContainer
        ),
        border = if (selected) BorderStroke(2.dp, Primary) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary)
        }
    }
}

// ─── Page 3: Schedule ────────────────────────────────────────────────────────

@Composable
private fun SchedulePage(vm: OnboardingViewModel) {
    var daysPerWeek by remember { mutableStateOf(vm.daysPerWeek) }
    var sessionMinutes by remember { mutableStateOf(vm.sessionMinutes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PageHeader(emoji = "📅", title = "Your schedule", subtitle = "How often can you train?")

        Column {
            Text(
                "Training days per week: $daysPerWeek",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            Text(
                when {
                    daysPerWeek <= 3 -> "Full body split"
                    daysPerWeek == 4 -> "Upper / lower split"
                    else -> "Push / pull / legs split"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Secondary
            )
            Slider(
                value = daysPerWeek.toFloat(),
                onValueChange = { daysPerWeek = it.toInt(); vm.daysPerWeek = it.toInt() },
                valueRange = 2f..6f,
                steps = 3
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("2 days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Text("6 days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }

        Column {
            Text(
                "Session length: $sessionMinutes minutes",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            Slider(
                value = sessionMinutes.toFloat(),
                onValueChange = { sessionMinutes = (it / 15).toInt() * 15; vm.sessionMinutes = sessionMinutes },
                valueRange = 30f..120f,
                steps = 5
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 min", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Text("2 hours", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}

// ─── Page 4: Equipment ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentPage(vm: OnboardingViewModel) {
    var selected by remember { mutableStateOf(vm.equipment.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(emoji = "🏋️", title = "Equipment", subtitle = "Select all that are available to you")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Equipment.entries.forEach { eq ->
                FilterChip(
                    selected = eq in selected,
                    onClick = {
                        selected = if (eq in selected) selected - eq else selected + eq
                        vm.equipment = selected.toList()
                    },
                    label = { Text(eq.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

// ─── Page 5: Strictness ──────────────────────────────────────────────────────

@Composable
private fun StrictnessPage(vm: OnboardingViewModel) {
    var strictness by remember { mutableStateOf(vm.strictness) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(emoji = "🎚️", title = "Coach style", subtitle = "How should your trainer communicate?")
        listOf(
            Triple(Strictness.GENTLE, "Gentle 🤗", "Encouraging, flexible, understanding. Perfect if you're just building the habit."),
            Triple(Strictness.NORMAL, "Normal 💬", "Balanced — supportive but direct. Holds you accountable without pressure."),
            Triple(Strictness.STRICT, "Strict 🔥", "No-excuses tone. Pushes harder. Best if you want maximum accountability.")
        ).forEach { (s, title, desc) ->
            GoalCard(
                emoji = "",
                title = title,
                description = desc,
                selected = strictness == s,
                onClick = { strictness = s; vm.strictness = s }
            )
        }
    }
}

// ─── Page 6: Summary ─────────────────────────────────────────────────────────

@Composable
private fun SummaryPage(vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("🎉", fontSize = 64.sp, textAlign = TextAlign.Center)
        Text(
            "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Here's your setup",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow("Goal", vm.goal.name.replace("_", " "))
                SummaryRow("Training", "${vm.daysPerWeek} days/week, ${vm.sessionMinutes} min each")
                SummaryRow("Experience", vm.experience.name.lowercase())
                SummaryRow("Coach style", vm.strictness.name.lowercase())
                SummaryRow("Equipment", "${vm.equipment.size} items selected")
            }
        }

        Text(
            "Your plan will be generated automatically.\nYou can change everything in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Text(value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.Medium)
    }
}

// ─── Shared composables ───────────────────────────────────────────────────────

@Composable
private fun PageHeader(emoji: String, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(top = 8.dp)) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
    }
}
