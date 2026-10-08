package com.badyetko.app

import kotlinx.serialization.Serializable

@Serializable
data class BudgetItem(
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val qty: Int = 1,
    val category: String = "Most Needed",
    val priority: String = "Medium",
    val notes: String = "",
    val purchased: Boolean = false
) { val total: Double get() = price * qty.coerceAtLeast(1) }

@Serializable
data class BudgetMonth(
    val salary: Double = 0.0,
    val extra: Double = 0.0,
    val saveRate: Double = 20.0,
    val items: List<BudgetItem> = emptyList()
)

@Serializable
data class BudgetState(
    val currency: String = "PHP",
    val months: Map<String, BudgetMonth> = emptyMap()
)

data class SharedBudget(
    val id: String,
    val ownerUserId: String,
    val ownerName: String,
    val ownerEmail: String,
    val permission: String,
    val shareMonth: String
)

enum class AppTheme(val label: String) {
    LIQUID_GLASS("Liquid Glass"), SOFT_FROST("Soft Frost"), WARM_MINIMAL("Warm Minimal"),
    PASTEL_FINANCE("Pastel Finance"), CLEAN_WHITE("Clean White"),
    SOFT_NEUMORPHIC("Soft Neumorphic"), PIXELATED("Pixelated")
}
