package com.personaltrainer.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.personaltrainer.ui.theme.*

/**
 * Plan screen — weekly view, session editor.
 * Full implementation in Phase 1.
 */
@Composable
fun PlanScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📅", style = MaterialTheme.typography.displayMedium)
            Text("Plan", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)
            Text("Your training plan will appear here", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }
    }
}
