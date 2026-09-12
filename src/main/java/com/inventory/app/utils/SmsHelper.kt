package com.inventory.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.inventory.app.database.DatabaseHelper
import com.inventory.app.models.InventoryItem
import com.inventory.app.models.User

/**
 * SmsHelper.kt - Helper utility for sending SMS notifications.
 *
 * Handles SMS permission checks and dispatches low-stock alerts
 * and test messages on behalf of the current user.
 *
 * @author Daniel Richmond
 * @version 2.0
 */
object SmsHelper {

    private const val TAG = "SmsHelper"

    /**
     * Checks if the app currently has the SEND_SMS runtime permission.
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Sends a single SMS message. Returns true if the dispatch call
     * did not throw; returns false on permission denial or send failure.
     */
    fun sendSms(context: Context, phoneNumber: String, message: String): Boolean {
        if (!hasSmsPermission(context)) {
            Log.w(TAG, "SMS permission not granted; cannot send to $phoneNumber")
            return false
        }

        if (phoneNumber.isBlank() || message.isBlank()) {
            Log.w(TAG, "Phone number or message is blank; skipping send")
            return false
        }

        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    phoneNumber, null, parts, null, null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber, null, message, null, null
                )
            }
            Log.d(TAG, "SMS dispatched to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            false
        }
    }

    /**
     * Sends a test SMS so the user can verify their settings.
     */
    fun sendTestNotification(context: Context, phoneNumber: String): Boolean {
        val message = "Inventory Manager: test notification. SMS alerts are working."
        return sendSms(context, phoneNumber, message)
    }

    /**
     * Checks the current user's inventory for low-stock items and, if any are
     * found and the user has opted in to low-stock alerts, sends a single
     * summary SMS listing them.
     */
    fun checkAndNotifyLowStock(context: Context, userId: Long) {
        if (!hasSmsPermission(context)) {
            Log.d(TAG, "Skipping low-stock notify: no SMS permission")
            return
        }

        val db = DatabaseHelper.getInstance(context)
        val user: User = db.getUserById(userId) ?: run {
            Log.w(TAG, "Skipping low-stock notify: user $userId not found")
            return
        }

        if (!user.canReceiveSms() || !user.isLowStockAlertsEnabled) {
            Log.d(TAG, "Skipping low-stock notify: user has not opted in")
            return
        }

        val lowStockItems: MutableList<InventoryItem> = db.getLowStockItems(userId)
        if (lowStockItems.isEmpty()) {
            Log.d(TAG, "No low-stock items for user $userId")
            return
        }

        val message = buildLowStockMessage(lowStockItems)
        val phone = user.phoneNumber ?: return
        sendSms(context, phone, message)
    }

    /**
     * Builds a human-readable summary of low-stock items, truncating
     * to keep the message under typical SMS limits.
     */
    private fun buildLowStockMessage(items: List<InventoryItem>): String {
        val sb = StringBuilder("Inventory Manager - Low Stock Alert:\n")

        val count = items.size.coerceAtMost(5)
        for (i in 0 until count) {
            val item = items[i]
            sb.append("- ")
                .append(item.name)
                .append(": ")
                .append(item.quantity)
                .append(" left (threshold ")
                .append(item.lowStockThreshold)
                .append(")\n")
        }

        if (items.size > count) {
            val remaining = items.size - count
            sb.append("...and ").append(remaining).append(" more low-stock item(s).")
        }

        return sb.toString().trim()
    }
}