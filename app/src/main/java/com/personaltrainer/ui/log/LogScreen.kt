package com.personaltrainer.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.personaltrainer.ui.theme.*

/**
 * Log screen — quick-entry hub for sets, meals, and body metrics.
 * Phase 1: stub. Full implementation in Phase 1/3.
 */
@Composable
fun LogScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Log", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)

        LogQuickActionCard(icon = Icons.Filled.FitnessCenter, title = "Log a set", subtitle = "Track your workout sets", onClick = {})
        LogQuickActionCard(icon = Icons.Filled.Restaurant, title = "Log a meal", subtitle = "What did you eat?", onClick = {})
        LogQuickActionCard(icon = Icons.Filled.Monitor, title = "Log weight", subtitle = "Today's body weight", onClick = {})
        LogQuickActionCard(icon = Icons.Filled.Bedtime, title = "Log sleep", subtitle = "Last night's sleep", onClick = {})
    }
}

@Composable
private fun LogQuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}
