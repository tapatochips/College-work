package com.inventory.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * SessionManager.java - Manages user session state using SharedPreferences.
 *
 * This class handles:
 * - Storing the current logged-in user's ID
 * - Checking if a user is logged in
 * - Logging out (clearing session)
 * @author Daniel Richmond
 * @version 2.0
 */
object SessionManager {
    // ==================== Constants ====================
    /** Tag for logging  */
    private const val TAG = "SessionManager"

    /** SharedPreferences file name  */
    private const val PREF_NAME = "inventory_session"

    /** Key for storing user ID  */
    private const val KEY_USER_ID = "user_id"

    /** Key for storing username (for display purposes)  */
    private const val KEY_USERNAME = "username"

    /** Key for tracking login state  */
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    /** Invalid user ID constant  */
    val INVALID_USER_ID: Long = -1

    // ==================== Session Methods ====================
    /**
     * Creates a new user session.
     * Stores the user's ID and username in SharedPreferences.
     *
     * @param context Application context
     * @param userId The logged-in user's database ID
     * @param username The logged-in user's username
     */
    fun createSession(context: Context, userId: Long, username: String?) {
        val prefs = getPreferences(context)
        val editor = prefs.edit()

        editor.putLong(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()

        Log.d(TAG, "Session created for user: " + username + " (ID: " + userId + ")")
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @param context Application context
     * @return true if a user is logged in, false otherwise
     */
    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPreferences(context)
        var isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)


        // another check to ensure user ID is valid
        if (isLoggedIn) {
            val userId = prefs.getLong(KEY_USER_ID, INVALID_USER_ID)
            isLoggedIn = (userId != INVALID_USER_ID)
        }

        return isLoggedIn
    }

    /**
     * Gets the current logged-in user's ID.
     *
     * @param context Application context
     * @return User ID if logged in, INVALID_USER_ID (-1) otherwise
     */
    fun getCurrentUserId(context: Context): Long {
        val prefs = getPreferences(context)
        return prefs.getLong(KEY_USER_ID, INVALID_USER_ID)
    }

    /**
     * Gets the current logged-in user's username.
     *
     * @param context Application context
     * @return Username if logged in, null otherwise
     */
    fun getCurrentUsername(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Ends the current user session (logout).
     * Clears all session data from SharedPreferences.
     *
     * @param context Application context
     */
    fun logout(context: Context) {
        val username = getCurrentUsername(context)

        val prefs = getPreferences(context)
        val editor = prefs.edit()


        // Clear all session data
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_USERNAME)
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.apply()

        Log.d(TAG, "Session ended for user: " + username)
    }

    /**
     * Clears all session data completely.
     * Use this for a complete reset.
     *
     * @param context Application context
     */
    fun clearAllData(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().clear().apply()

        Log.d(TAG, "All session data cleared")
    }

    // ==================== Private Helper Methods ====================
    /**
     * Gets the SharedPreferences instance.
     *
     * @param context Application context
     * @return SharedPreferences instance for session data
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}