package com.personaltrainer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.personaltrainer.ui.theme.*

/**
 * Settings screen — API keys, permissions, data management.
 * Full settings will grow with each phase.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)

        // AI section
        SettingsSection(title = "AI Provider") {
            var apiKey by remember { mutableStateOf("") }
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key (AI Studio)") },
                supportingText = { Text("Optional — free tier. Leave blank to use template responses.") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Filled.Key, contentDescription = null) }
            )
        }

        // Reminders section
        SettingsSection(title = "Reminders") {
            var notificationsEnabled by remember { mutableStateOf(true) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable notifications", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    Text("Workout reminders and check-ins", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }
        }

        // Reminder health section
        SettingsSection(title = "Reminder Health") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionRow("POST_NOTIFICATIONS", true)
                    PermissionRow("SCHEDULE_EXACT_ALARM", true)
                    PermissionRow("Battery Unrestricted", false)  // User needs to set manually
                }
            }
            Text(
                "⚠️ For reliable reminders, set battery to 'Unrestricted' in Android Settings → Apps → Personal Trainer → Battery.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        // Data section
        SettingsSection(title = "Data") {
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export data (JSON)")
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Tertiary)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete all my data")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Primary)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceContainer), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (granted) Secondary else Tertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = if (granted) OnSurface else OnSurfaceVariant)
    }
}
