package com.gaitguard.app.util

import android.content.Context
import java.util.Calendar

/**
 * Single source of truth for the app owner's display name and the
 * time-based greeting shown on the Metrics dashboard.
 *
 * The name defaults to the project owner and can be overridden via the
 * SharedPreferences key `user_name` (same store as the rest of settings).
 */
object UserProfile {

    private const val PREFS = "gaitguard_settings"
    private const val KEY_NAME = "user_name"
    private const val DEFAULT_NAME = "Dong Kun Moon"

    /** Full display name, e.g. "Dong Kun Moon". */
    fun displayName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, DEFAULT_NAME)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME

    fun setDisplayName(context: Context, name: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()

    /** Given name used in the greeting — everything except the final surname token. */
    fun greetingName(context: Context): String {
        val parts = displayName(context).trim().split(Regex("\\s+"))
        return when {
            parts.size >= 2 -> parts.dropLast(1).joinToString(" ")
            else            -> parts.firstOrNull().orEmpty()
        }
    }

    /** "Good morning, Dong Kun" / "Good afternoon, …" / "Good evening, …". */
    fun greeting(context: Context, hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        val part = when (hour) {
            in 5..11  -> "Good morning"
            in 12..17 -> "Good afternoon"
            else      -> "Good evening"
        }
        val name = greetingName(context)
        return if (name.isBlank()) part else "$part, $name"
    }
}
