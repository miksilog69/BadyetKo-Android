package com.badyetko.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun NumberBox(label: String, value: Double, modifier: Modifier, onValue: (Double) -> Unit) = OutlinedTextField(
    value = if (value == 0.0) "" else value.toString().removeSuffix(".0"),
    onValueChange = { onValue(it.toDoubleOrNull() ?: 0.0) },
    modifier = modifier, label = { Text(label) }, singleLine = true
)

@Composable
fun Metric(label: String, value: Double, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(12.dp)) {
    Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Text("₱%,.0f".format(value), fontSize = 18.sp, fontWeight = FontWeight.Black)
} } }

@Composable
fun BudgetRow(item: BudgetItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) { Card { Row(
    Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(item.purchased, onToggle); Spacer(Modifier.width(8.dp))
    Column(Modifier.weight(1f)) {
        Text(item.name, fontWeight = FontWeight.Bold, textDecoration = if (item.purchased) TextDecoration.LineThrough else null)
        Text("${item.category} • ${item.priority}", style = MaterialTheme.typography.labelSmall)
    }
    Text("₱%,.0f".format(item.total), fontWeight = FontWeight.Bold)
    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
} } }

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onSave: (BudgetItem) -> Unit) {
    var name by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var qty by remember { mutableStateOf("1") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add item") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Item name") })
        OutlinedTextField(price, { price = it }, label = { Text("Price") })
        OutlinedTextField(qty, { qty = it }, label = { Text("Quantity") })
    } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = {
        onSave(BudgetItem(UUID.randomUUID().toString(), name, price.toDoubleOrNull() ?: 0.0, qty.toIntOrNull()?.coerceAtLeast(1) ?: 1))
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
