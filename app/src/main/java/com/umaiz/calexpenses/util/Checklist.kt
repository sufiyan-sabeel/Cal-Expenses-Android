package com.umaiz.calexpenses.util

import com.umaiz.calexpenses.data.ChecklistItem
import org.json.JSONArray
import org.json.JSONObject

object Checklist {
    fun toJson(items: List<ChecklistItem>): String {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("text", it.text).put("done", it.done)) }
        return arr.toString()
    }

    fun fromJson(s: String): MutableList<ChecklistItem> {
        val out = mutableListOf<ChecklistItem>()
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(ChecklistItem(o.optString("text"), o.optBoolean("done")))
            }
        } catch (_: Exception) { }
        return out
    }
}
