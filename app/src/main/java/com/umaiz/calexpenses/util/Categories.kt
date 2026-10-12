package com.umaiz.calexpenses.util

object Categories {
    val EXPENSE = listOf(
        "Food", "Transport", "Bills", "Shopping", "Health",
        "Entertainment", "Education", "Rent", "Groceries", "Other"
    )
    val INCOME = listOf("Salary", "Business", "Gift", "Interest", "Other")
    val TRANSFER = listOf("Transfer", "Savings", "Investment", "Other")

    fun forType(type: String): List<String> = when (type) {
        "INCOME" -> INCOME
        "TRANSFER" -> TRANSFER
        else -> EXPENSE
    }
}
