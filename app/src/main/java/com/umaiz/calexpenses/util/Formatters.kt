package com.umaiz.calexpenses.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    var currencySymbol: String = "\u20B9"   // ₹ by default (India)

    fun money(v: Double): String =
        currencySymbol + String.format(Locale.getDefault(), "%,.2f", v)

    fun moneyShort(v: Double): String {
        val abs = Math.abs(v)
        return when {
            abs >= 10000000 -> currencySymbol + String.format(Locale.getDefault(), "%.2fCr", v / 10000000)
            abs >= 100000 -> currencySymbol + String.format(Locale.getDefault(), "%.2fL", v / 100000)
            abs >= 1000 -> currencySymbol + String.format(Locale.getDefault(), "%.1fk", v / 1000)
            else -> currencySymbol + String.format(Locale.getDefault(), "%.0f", v)
        }
    }

    private fun fmt(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    fun date(millis: Long): String = fmt("dd MMM yyyy").format(Date(millis))
    fun dateTime(millis: Long): String = fmt("dd MMM yyyy, hh:mm a").format(Date(millis))

    fun monthKey(millis: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(millis))

    fun currentMonthKey(): String = monthKey(System.currentTimeMillis())

    fun monthLabel(key: String): String {
        return try {
            val d = SimpleDateFormat("yyyy-MM", Locale.US).parse(key)!!
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d)
        } catch (_: Exception) { key }
    }

    fun shiftMonth(key: String, delta: Int): String {
        val d = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
        val c = Calendar.getInstance(); c.time = d; c.add(Calendar.MONTH, delta)
        return SimpleDateFormat("yyyy-MM", Locale.US).format(c.time)
    }

    fun startOfMonth(millis: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = millis
        c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfMonth(millis: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = millis
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
