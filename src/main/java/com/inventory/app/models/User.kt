package com.inventory.app.models

/**
 * User.kt - Model class representing a user account in the inventory system.
 *
 * This class encapsulates all user-related data including authentication
 * credentials and SMS notification preferences.
 *
 * Database Table: users
 * - id: Primary key, auto-increment
 * - username: Unique identifier for login
 * - password_hash: Hashed password for security
 * - phone_number: Optional phone for SMS notifications
 * - sms_enabled: Whether SMS notifications are enabled
 * - low_stock_alerts: Preference for low stock SMS alerts
 * - event_alerts: Preference for event SMS alerts
 *
 * @author Daniel Richmond
 * @version 1.0
 */
class User() {

    // ==================== Properties ====================

    /** Unique identifier for the user (primary key in database) */
    var id: Long = 0

    /** Username for authentication */
    var username: String? = null

    /** Hashed password */
    var passwordHash: String? = null

    /** Phone number for receiving SMS notifications */
    var phoneNumber: String? = null

    /** Master toggle for SMS notification feature */
    var isSmsEnabled: Boolean = false

    /** Individual preference for low stock alerts */
    var isLowStockAlertsEnabled: Boolean = true

    /** Individual preference for event reminders */
    var isEventAlertsEnabled: Boolean = true

    /** Individual preference for goal achievement notifications */
    var isGoalAlertsEnabled: Boolean = false

    /** Timestamp when the account was created */
    var createdAt: Long = 0

    /** Timestamp of the last successful login */
    var lastLoginAt: Long = 0

    // ==================== Constructors ====================

    /**
     * Default constructor - initializes with default preference values.
     * SMS is disabled by default, but individual alert types are enabled
     * so they work immediately when the user grants SMS permission.
     */
    init {
        this.createdAt = System.currentTimeMillis()
    }

    /**
     * Constructor for creating a new user with credentials.
     * Used during account registration.
     *
     * @param username The unique username for login
     * @param passwordHash The hashed password (never store plain text!)
     */
    constructor(username: String?, passwordHash: String?) : this() {
        this.username = username
        this.passwordHash = passwordHash
    }

    /**
     * Full constructor for loading user from database.
     *
     * @param id Database primary key
     * @param username User's login name
     * @param passwordHash Hashed password
     * @param phoneNumber Phone for SMS (can be null)
     * @param smsEnabled Whether SMS notifications are active
     */
    constructor(
        id: Long, username: String?, passwordHash: String?,
        phoneNumber: String?, smsEnabled: Boolean
    ) : this() {
        this.id = id
        this.username = username
        this.passwordHash = passwordHash
        this.phoneNumber = phoneNumber
        this.isSmsEnabled = smsEnabled
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if the user has a valid phone number configured.
     * A valid phone number is non-null and not blank.
     *
     * @return true if a phone number is configured, false otherwise
     */
    fun hasPhoneNumber(): Boolean {
        return !phoneNumber.isNullOrBlank()
    }

    /**
     * Determines if SMS notifications can be sent to this user.
     * Both conditions must be met: SMS must be enabled AND a valid
     * phone number must be configured.
     *
     * @return true if SMS can be sent, false otherwise
     */
    fun canReceiveSms(): Boolean {
        return isSmsEnabled && hasPhoneNumber()
    }

    /**
     * String representation for debugging purposes.
     * Note: Does NOT include password hash for security.
     */
    override fun toString(): String {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", phoneNumber='" + (if (phoneNumber != null) "***" else "null") + '\'' +
                ", smsEnabled=" + isSmsEnabled +
                '}'
    }

    /**
     * Equality check based on database ID.
     * Two users are equal if they have the same ID.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val user = other as User
        return id == user.id
    }

    /**
     * Hash code based on database ID.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }
}