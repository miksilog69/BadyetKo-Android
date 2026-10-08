package com.badyetko.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetScreen(store: LocalStore, api: ApiClient, signedIn: Boolean, theme: AppTheme) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(store.loadBudget()) }
    var monthKey by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var syncText by remember { mutableStateOf(if (signedIn) "Cloud connected" else "Local mode") }
    var addOpen by remember { mutableStateOf(false) }
    val month = state.months[monthKey] ?: BudgetMonth()

    fun save(next: BudgetState) {
        state = next; store.saveBudget(next)
        if (signedIn) scope.launch {
            syncText = "Saving…"
            runCatching { withContext(Dispatchers.IO) { api.saveBudget(next) } }
                .onSuccess { syncText = "Cloud connected" }.onFailure { syncText = "Saved locally" }
        }
    }
    fun saveMonth(next: BudgetMonth) = save(state.copy(months = state.months + (monthKey to next)))

    LaunchedEffect(signedIn) {
        if (signedIn) {
            syncText = "Syncing…"
            runCatching { withContext(Dispatchers.IO) { api.loadBudget() } }
                .onSuccess { cloud -> if (cloud.months.isNotEmpty()) { state = cloud; store.saveBudget(cloud) }; syncText = "Cloud connected" }
                .onFailure { syncText = "Local mode" }
        }
    }

    val income = month.salary + month.extra
    val spent = month.items.filter { it.purchased }.sumOf { it.total }
    val planned = month.items.filterNot { it.purchased }.sumOf { it.total }
    val savings = income * month.saveRate / 100.0
    val available = income - spent - savings

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AssistChip(onClick = {}, label = { Text("LIVE BUDGET • $syncText", fontWeight = FontWeight.Bold) }) }
        item {
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(monthKey, { monthKey = it }, label = { Text("Month (YYYY-MM)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberBox("Salary", month.salary, Modifier.weight(1f)) { saveMonth(month.copy(salary = it)) }
                    NumberBox("Extra", month.extra, Modifier.weight(1f)) { saveMonth(month.copy(extra = it)) }
                }
                NumberBox("Savings %", month.saveRate, Modifier.fillMaxWidth()) { saveMonth(month.copy(saveRate = it.coerceIn(0.0, 100.0))) }
            } }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Income", income, Modifier.weight(1f)); Metric("Spent", spent, Modifier.weight(1f)); Metric("Available", available, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Planned", planned, Modifier.weight(1f)); Metric("Savings", savings, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Things to buy", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Checked = Purchased") }
            Button(onClick = { addOpen = true }) { Icon(Icons.Outlined.Add, null); Text("Add") }
        } }
        items(month.items, key = { it.id }) { item -> BudgetRow(item,
            onToggle = { checked -> saveMonth(month.copy(items = month.items.map { if (it.id == item.id) it.copy(purchased = checked) else it })) },
            onDelete = { saveMonth(month.copy(items = month.items.filterNot { it.id == item.id })) }) }
    }

    if (addOpen) AddItemDialog({ addOpen = false }) { item -> saveMonth(month.copy(items = month.items + item)); addOpen = false }
}
