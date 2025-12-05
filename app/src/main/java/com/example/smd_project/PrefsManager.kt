package com.example.smd_project

import android.content.Context
import java.util.*

class PrefsManager(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("smd_prefs", Context.MODE_PRIVATE)

    fun getUserId(): String {
        var id = prefs.getString("userId", null)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("userId", id).apply()
        }
        return id
    }

    fun setUserId(id: String) {
        prefs.edit().putString("userId", id).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
