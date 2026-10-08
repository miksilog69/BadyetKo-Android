package com.badyetko.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppTab { BUDGET, SHARED, ALERTS, SETTINGS }

class MainActivity : ComponentActivity() {
    private lateinit var store: LocalStore
    private lateinit var api: ApiClient
    private lateinit var auth: AuthManager
    private var authVersion by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = LocalStore(this)
        api = ApiClient(store)
        auth = AuthManager(this, store)
        if (auth.handleIntent(intent)) authVersion++
        setContent { BadyetKoApp(store, api, auth, authVersion) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (auth.handleIntent(intent)) authVersion++
    }
}

@Composable
fun BadyetKoApp(store: LocalStore, api: ApiClient, auth: AuthManager, authVersion: Int) {
    var theme by remember { mutableStateOf(store.loadTheme()) }
    var tab by remember { mutableStateOf(AppTab.BUDGET) }
    var signedIn by remember(authVersion) { mutableStateOf(auth.signedIn()) }

    MaterialTheme(colorScheme = badyetKoColors(theme)) {
        Scaffold(
            topBar = { BadyetKoTopBar(signedIn, { auth.signInGoogle() }, { auth.signOut(); signedIn = false }) },
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple(AppTab.BUDGET, Icons.Outlined.AccountBalanceWallet, "Budget"),
                        Triple(AppTab.SHARED, Icons.Outlined.People, "Shared"),
                        Triple(AppTab.ALERTS, Icons.Outlined.Notifications, "Alerts"),
                        Triple(AppTab.SETTINGS, Icons.Outlined.Settings, "Settings")
                    )
                    items.forEach { (target, icon, label) ->
                        NavigationBarItem(
                            selected = tab == target,
                            onClick = { tab = target },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding).background(badyetKoBackground(theme))
            ) {
                when (tab) {
                    AppTab.BUDGET -> BudgetScreen(store, api, signedIn, theme)
                    AppTab.SHARED -> SharedScreen(api, signedIn)
                    AppTab.ALERTS -> AlertsScreen(api, signedIn)
                    AppTab.SETTINGS -> SettingsScreen(
                        theme = theme,
                        onTheme = { theme = it; store.saveTheme(it) },
                        signedIn = signedIn,
                        onSignIn = { auth.signInGoogle() },
                        onSignOut = { auth.signOut(); signedIn = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun BadyetKoTopBar(signedIn: Boolean, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(
                Brush.linearGradient(listOf(Color(0xFF12D9D0), Color(0xFF1769F4), Color(0xFF7B2CF3))),
                RoundedCornerShape(12.dp)
            ), contentAlignment = Alignment.Center
        ) { Text("B", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("BadyetKo", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Native Android", style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalIconButton(onClick = if (signedIn) onSignOut else onSignIn) {
            Icon(if (signedIn) Icons.Outlined.Logout else Icons.Outlined.Login, contentDescription = null)
        }
    }
}
