package com.umaiz.calexpenses.ui.transactions

import com.umaiz.calexpenses.data.ExpenseEntity

/** A row in the transactions list: either a date header or a transaction. */
sealed class TxRow {
    data class Header(val label: String) : TxRow()
    data class Item(val expense: ExpenseEntity) : TxRow()
}
