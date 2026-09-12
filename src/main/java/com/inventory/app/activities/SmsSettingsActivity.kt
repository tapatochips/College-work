package com.inventory.app.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.inventory.app.R
import com.inventory.app.database.DatabaseHelper
import com.inventory.app.models.User
import com.inventory.app.utils.SessionManager
import com.inventory.app.utils.SmsHelper

/**
 * SmsSettingsActivity.kt - Screen for managing SMS notification preferences.
 *
 * Lets the user grant the SMS permission, store a phone number and toggle
 * which alert types they want to receive.
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class SmsSettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var ivPermissionIcon: ImageView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var ivStatusIndicator: ImageView
    private lateinit var btnRequestPermission: MaterialButton

    private lateinit var switchLowStock: SwitchMaterial
    private lateinit var switchEvents: SwitchMaterial

    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var btnSavePhone: MaterialButton
    private lateinit var btnTestSms: MaterialButton

    private lateinit var databaseHelper: DatabaseHelper
    private var currentUserId: Long = 0
    private var currentUser: User? = null

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_settings)

        currentUserId = SessionManager.getCurrentUserId(this)
        databaseHelper = DatabaseHelper.getInstance(this)

        loadUserData()

        initializeViews()
        setupToolbar()
        setupClickListeners()
        updatePermissionUI()
        populateUserPreferences()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun loadUserData() {
        currentUser = databaseHelper.getUserById(currentUserId)
        if (currentUser == null) {
            Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivPermissionIcon = findViewById(R.id.ivPermissionIcon)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        ivStatusIndicator = findViewById(R.id.ivStatusIndicator)
        btnRequestPermission = findViewById(R.id.btnRequestPermission)

        switchLowStock = findViewById(R.id.switchLowStock)
        switchEvents = findViewById(R.id.switchEvents)

        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSavePhone = findViewById(R.id.btnSavePhone)
        btnTestSms = findViewById(R.id.btnTestSms)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun populateUserPreferences() {
        val user = currentUser ?: return

        if (user.hasPhoneNumber()) {
            etPhoneNumber.setText(user.phoneNumber)
        }

        switchLowStock.isChecked = user.isLowStockAlertsEnabled
        switchEvents.isChecked = user.isEventAlertsEnabled
    }

    private fun setupClickListeners() {
        btnRequestPermission.setOnClickListener { requestSmsPermission() }
        btnSavePhone.setOnClickListener { savePhoneNumber() }
        btnTestSms.setOnClickListener { sendTestSms() }

        switchLowStock.setOnCheckedChangeListener { _, _ ->
            saveNotificationPreferences()
        }

        switchEvents.setOnCheckedChangeListener { _, _ ->
            saveNotificationPreferences()
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        if (hasSmsPermission()) {
            onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun onPermissionGranted() {
        currentUser?.isSmsEnabled = true
        saveNotificationPreferences()
        updatePermissionUI()
        Toast.makeText(this, R.string.permission_granted_message, Toast.LENGTH_SHORT).show()
    }

    private fun onPermissionDenied() {
        currentUser?.isSmsEnabled = false
        saveNotificationPreferences()
        updatePermissionUI()
        Toast.makeText(this, R.string.permission_denied_message, Toast.LENGTH_LONG).show()
    }

    private fun updatePermissionUI() {
        if (hasSmsPermission()) {
            tvPermissionStatus.setText(R.string.permission_granted)
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.success))

            ivStatusIndicator.setImageResource(R.drawable.ic_check_circle)
            ivStatusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.success))

            btnRequestPermission.text = "Permission Granted"
            btnRequestPermission.isEnabled = false

            enableNotificationControls(true)
        } else {
            tvPermissionStatus.setText(R.string.permission_not_granted)
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))

            ivStatusIndicator.setImageResource(R.drawable.ic_warning)
            ivStatusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.warning))

            btnRequestPermission.setText(R.string.grant_permission)
            btnRequestPermission.isEnabled = true

            enableNotificationControls(false)
        }
    }

    private fun enableNotificationControls(enabled: Boolean) {
        switchLowStock.isEnabled = enabled
        switchEvents.isEnabled = enabled
        btnTestSms.isEnabled = enabled

        val alpha = if (enabled) 1.0f else 0.5f
        switchLowStock.alpha = alpha
        switchEvents.alpha = alpha
    }

    private fun savePhoneNumber() {
        val phoneNumber = this.phoneNumber

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, R.string.enter_phone_number, Toast.LENGTH_SHORT).show()
            return
        }

        currentUser?.phoneNumber = phoneNumber
        saveNotificationPreferences()

        Toast.makeText(this, R.string.phone_saved, Toast.LENGTH_SHORT).show()
    }

    private fun saveNotificationPreferences() {
        val user = currentUser ?: return

        val phoneNumber = this.phoneNumber
        val smsEnabled = hasSmsPermission()
        val lowStockAlerts: Boolean = switchLowStock.isChecked
        val eventAlerts: Boolean = switchEvents.isChecked

        val success: Boolean = databaseHelper.updateUserSmsPreferences(
            currentUserId,
            phoneNumber,
            smsEnabled,
            lowStockAlerts,
            eventAlerts,
            false
        )

        if (success) {
            user.phoneNumber = phoneNumber
            user.isSmsEnabled = smsEnabled
            user.isLowStockAlertsEnabled = lowStockAlerts
            user.isEventAlertsEnabled = eventAlerts
            user.isGoalAlertsEnabled = false
        }
    }

    private fun sendTestSms() {
        if (!hasSmsPermission()) {
            Toast.makeText(this, R.string.test_notification_failed, Toast.LENGTH_SHORT).show()
            requestSmsPermission()
            return
        }

        val phoneNumber = this.phoneNumber

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, R.string.enter_phone_number, Toast.LENGTH_SHORT).show()
            return
        }

        val success: Boolean = SmsHelper.sendTestNotification(this, phoneNumber)

        if (success) {
            Toast.makeText(this, R.string.test_notification_sent, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.test_notification_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private val phoneNumber: String
        get() = etPhoneNumber.text?.toString()?.trim() ?: ""
}