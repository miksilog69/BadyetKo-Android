package com.badyetko.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

@Composable
fun SharedScreen(api: ApiClient, signedIn: Boolean) {
    var data by remember { mutableStateOf<List<SharedBudget>>(emptyList()) }
    var message by remember { mutableStateOf(if (signedIn) "Loading…" else "Sign in to see shared budgets") }
    LaunchedEffect(signedIn) {
        if (signedIn) runCatching { withContext(Dispatchers.IO) { api.sharedBudgets() } }
            .onSuccess { data = it; message = if (it.isEmpty()) "No shared budgets yet" else "" }
            .onFailure { message = it.message ?: "Could not load shared budgets" }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Shared Budgets", fontSize = 26.sp, fontWeight = FontWeight.Black) }
        if (message.isNotBlank()) item { Text(message) }
        items(data) { shared -> Card { Column(Modifier.padding(16.dp)) { Text(shared.ownerName, fontWeight = FontWeight.Bold); Text(shared.ownerEmail); Text("${shared.permission} • ${shared.shareMonth.ifBlank { "Assigned month" }}") } } }
    }
}

@Composable
fun AlertsScreen(api: ApiClient, signedIn: Boolean) {
    var text by remember { mutableStateOf(if (signedIn) "Loading…" else "Sign in to see notifications") }
    LaunchedEffect(signedIn) {
        if (signedIn) text = runCatching { withContext(Dispatchers.IO) {
            val c = api.collabDashboard(); val ch = api.challenges(); val a = api.activity()
            val invites = (c["incoming"] as? JsonArray)?.size ?: 0
            val challenges = (ch["challenges"] as? JsonArray)?.size ?: 0
            val notifications = (a["notifications"] as? JsonArray)?.size ?: 0
            "Sharing invites: $invites\nChallenges: $challenges\nActivity notifications: $notifications"
        } }.getOrElse { it.message ?: "Could not refresh notifications" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Notifications", fontSize = 26.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); Card { Text(text, Modifier.padding(16.dp)) } }
}

@Composable
fun SettingsScreen(theme: AppTheme, onTheme: (AppTheme) -> Unit, signedIn: Boolean, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Black) }
        item { Card { Column(Modifier.padding(16.dp)) { Text("Theme", fontWeight = FontWeight.Bold); AppTheme.entries.forEach { t -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { RadioButton(theme == t, { onTheme(t) }); Text(t.label) } } } } }
        item { Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Cloud account", fontWeight = FontWeight.Bold); Text(if (signedIn) "Connected" else "Not signed in") }; Button(onClick = if (signedIn) onSignOut else onSignIn) { Text(if (signedIn) "Sign out" else "Google sign-in") } } } }
    }
}
