package com.inventory.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.inventory.app.models.InventoryItem
import com.inventory.app.models.User
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * DatabaseHelper.kt - SQLite database manager for the Inventory App.
 *
 * This class handles all database operations including:
 * - Database creation and version management
 * - User authentication (login/registration)
 * - CRUD operations for inventory items
 * - User preference management
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class DatabaseHelper
/**
 * Private constructor, prevents direct instantiation.
 *
 * @param context Application context
 */
private constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // ==================== Database Lifecycle Methods ====================
    /**
     * Called when the db is created for the first time.
     * Creates all tables with their schema.
     *
     * @param db The db instance
     */
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "Creating database tables...")

        // Users table
        val createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_USER_PASSWORD + " TEXT NOT NULL, " +
                COL_USER_PHONE + " TEXT, " +
                COL_USER_SMS_ENABLED + " INTEGER DEFAULT 0, " +
                COL_USER_LOW_STOCK_ALERTS + " INTEGER DEFAULT 1, " +
                COL_USER_EVENT_ALERTS + " INTEGER DEFAULT 1, " +
                COL_USER_GOAL_ALERTS + " INTEGER DEFAULT 0, " +
                COL_USER_CREATED_AT + " INTEGER, " +
                COL_USER_LAST_LOGIN + " INTEGER" +
                ")"

        // Inventory Items table with foreign key to Users
        val createInventoryTable = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_USER_ID + " INTEGER NOT NULL, " +
                COL_ITEM_NAME + " TEXT NOT NULL, " +
                COL_ITEM_DESCRIPTION + " TEXT, " +
                COL_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                COL_ITEM_PRICE + " REAL NOT NULL DEFAULT 0.0, " +
                COL_ITEM_THRESHOLD + " INTEGER DEFAULT 5, " +
                COL_ITEM_CREATED_AT + " INTEGER, " +
                COL_ITEM_UPDATED_AT + " INTEGER, " +
                "FOREIGN KEY (" + COL_ITEM_USER_ID + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE" +
                ")"

        // Execute table creation
        db.execSQL(createUsersTable)
        Log.d(TAG, "Users table created successfully")

        db.execSQL(createInventoryTable)
        Log.d(TAG, "Inventory table created successfully")
    }

    /**
     * Called when the database needs to be upgraded.
     * Handles schema migrations between versions.
     * @param db The database instance
     * @param oldVersion Previous database version
     * @param newVersion New database version
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion)

        // drop existing tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS)

        // recreate tables with new schema
        onCreate(db)
    }

    /**
     * Enable foreign key constraints.
     * Called when database connection is configured.
     *
     * @param db The database instance
     */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ==================== Password Hashing ====================
    /**
     * Hashes a password using SHA-256 algorithm.
     * @param password Plain text password to hash
     * @return Hexadecimal string representation of the hash
     */
    fun hashPassword(password: String): String? {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray())

            // convert bytes to hexadecimal string
            val hexString = StringBuilder()
            for (b in hashBytes) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Error hashing password: SHA-256 not available", e)
            // fallback, this isn't secure but good for debugging and prevents crashes
            return password
        }
    }

    // ==================== User Operations ====================
    /**
     * Creates a new user account in the database.
     * Password is hashed before storage for security.
     * @param username Unique username for the account
     * @param password Plain text password (will be hashed)
     * @return The newly created User object with ID, or null if creation failed
     */
    fun createUser(username: String?, password: String): User? {
        // check if username already exists
        if (getUserByUsername(username) != null) {
            Log.w(TAG, "Cannot create user: username '" + username + "' already exists")
            return null
        }

        val db = this.writableDatabase

        // prep user data
        val values = ContentValues()
        values.put(COL_USER_USERNAME, username)
        values.put(COL_USER_PASSWORD, hashPassword(password))
        values.put(COL_USER_SMS_ENABLED, 0)
        values.put(COL_USER_LOW_STOCK_ALERTS, 1)
        values.put(COL_USER_EVENT_ALERTS, 1)
        values.put(COL_USER_GOAL_ALERTS, 0)
        values.put(COL_USER_CREATED_AT, System.currentTimeMillis())
        values.put(COL_USER_LAST_LOGIN, System.currentTimeMillis())

        // insert and get the new row ID
        val userId = db.insert(TABLE_USERS, null, values)

        if (userId == -1L) {
            Log.e(TAG, "Failed to create user: " + username)
            return null
        }

        Log.d(TAG, "User created successfully: " + username + " (ID: " + userId + ")")

        // the newly created user
        return getUserById(userId)
    }

    /**
     * Authenticates a user with username and password.
     * Compares password hash against stored hash.
     * Updates last login timestamp on successful authentication.
     *
     * @param username The username to authenticate
     * @param password Plain text password to verify
     * @return User object if authentication successful, null otherwise
     */
    fun authenticateUser(username: String?, password: String): User? {
        val db = this.readableDatabase

        // hash the provided password for comparison
        val passwordHash = hashPassword(password)

        // search for matching user
        val columns = arrayOf(
            COL_USER_ID, COL_USER_USERNAME, COL_USER_PASSWORD,
            COL_USER_PHONE, COL_USER_SMS_ENABLED, COL_USER_LOW_STOCK_ALERTS,
            COL_USER_EVENT_ALERTS, COL_USER_GOAL_ALERTS, COL_USER_CREATED_AT,
            COL_USER_LAST_LOGIN
        )

        val selection = COL_USER_USERNAME + " = ? AND " + COL_USER_PASSWORD + " = ?"
        val selectionArgs = arrayOf(username, passwordHash)

        val cursor = db.query(
            TABLE_USERS, columns, selection, selectionArgs,
            null, null, null
        )

        var user: User? = null

        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
            Log.d(TAG, "User authenticated: " + username)

            // update last login timestamp
            updateLastLogin(user.id)
        } else {
            Log.w(TAG, "Authentication failed for user: " + username)
        }

        cursor.close()
        return user
    }

    /**
     * Retrieves a user by their unique ID.
     * @param userId The user's database ID
     * @return User object if found, null otherwise
     */
    fun getUserById(userId: Long): User? {
        val db = this.readableDatabase

        val columns = arrayOf(
            COL_USER_ID, COL_USER_USERNAME, COL_USER_PASSWORD,
            COL_USER_PHONE, COL_USER_SMS_ENABLED, COL_USER_LOW_STOCK_ALERTS,
            COL_USER_EVENT_ALERTS, COL_USER_GOAL_ALERTS, COL_USER_CREATED_AT,
            COL_USER_LAST_LOGIN
        )

        val selection = COL_USER_ID + " = ?"
        val selectionArgs = arrayOf(userId.toString())

        val cursor = db.query(
            TABLE_USERS, columns, selection, selectionArgs,
            null, null, null
        )

        var user: User? = null

        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }

        cursor.close()
        return user
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username to search for
     * @return User object if found, null otherwise
     */
    fun getUserByUsername(username: String?): User? {
        val db = this.readableDatabase

        val columns = arrayOf(
            COL_USER_ID, COL_USER_USERNAME, COL_USER_PASSWORD,
            COL_USER_PHONE, COL_USER_SMS_ENABLED, COL_USER_LOW_STOCK_ALERTS,
            COL_USER_EVENT_ALERTS, COL_USER_GOAL_ALERTS, COL_USER_CREATED_AT,
            COL_USER_LAST_LOGIN
        )

        val selection = COL_USER_USERNAME + " = ?"
        val selectionArgs = arrayOf(username)

        val cursor = db.query(
            TABLE_USERS, columns, selection, selectionArgs,
            null, null, null
        )

        var user: User? = null

        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }

        cursor.close()
        return user
    }

    /**
     * Updates a user's last login timestamp to current time.
     *
     * @param userId The user's database ID
     */
    private fun updateLastLogin(userId: Long) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_USER_LAST_LOGIN, System.currentTimeMillis())

        db.update(
            TABLE_USERS, values, COL_USER_ID + " = ?",
            arrayOf(userId.toString())
        )
    }

    /**
     * Updates user's SMS preferences.
     *
     * @param userId The user's database ID
     * @param phoneNumber Phone number for SMS notifications
     * @param smsEnabled Master SMS toggle
     * @param lowStockAlerts Low stock alert preference
     * @param eventAlerts Event alert preference
     * @param goalAlerts Goal achievement alert preference
     * @return true if update successful, false otherwise
     */
    fun updateUserSmsPreferences(
        userId: Long, phoneNumber: String?,
        smsEnabled: Boolean, lowStockAlerts: Boolean,
        eventAlerts: Boolean, goalAlerts: Boolean
    ): Boolean {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_USER_PHONE, phoneNumber)
        values.put(COL_USER_SMS_ENABLED, if (smsEnabled) 1 else 0)
        values.put(COL_USER_LOW_STOCK_ALERTS, if (lowStockAlerts) 1 else 0)
        values.put(COL_USER_EVENT_ALERTS, if (eventAlerts) 1 else 0)
        values.put(COL_USER_GOAL_ALERTS, if (goalAlerts) 1 else 0)

        val rowsAffected = db.update(
            TABLE_USERS, values, COL_USER_ID + " = ?",
            arrayOf(userId.toString())
        )

        Log.d(TAG, "Updated SMS preferences for user " + userId + ": " + rowsAffected + " row(s)")
        return rowsAffected > 0
    }

    /**
     * Converts a database cursor to a User object.
     * Helper method to avoid code duplication.
     *
     * @param cursor Cursor positioned at a user row
     * @return User object populated with cursor data
     */
    private fun cursorToUser(cursor: Cursor): User {
        val user = User()

        user.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID))
        user.username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_USERNAME))
        user.passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD))
        user.phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PHONE))
        user.isSmsEnabled =
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_SMS_ENABLED)) == 1
        user.isLowStockAlertsEnabled =
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_LOW_STOCK_ALERTS)) == 1
        user.isEventAlertsEnabled =
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_EVENT_ALERTS)) == 1
        user.isGoalAlertsEnabled =
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_GOAL_ALERTS)) == 1
        user.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT))
        user.lastLoginAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_LAST_LOGIN))

        return user
    }

    // ==================== Inventory CRUD Operations ====================
    /**
     * CREATE: Adds a new inventory item to the database.
     *
     * @param item The InventoryItem to add (id field is ignored)
     * @return The ID of the newly created item, or -1 if creation failed
     */
    fun addInventoryItem(item: InventoryItem): Long {
        val db = this.writableDatabase

        val currentTime = System.currentTimeMillis()

        val values = ContentValues()
        values.put(COL_ITEM_USER_ID, item.userId)
        values.put(COL_ITEM_NAME, item.name)
        values.put(COL_ITEM_DESCRIPTION, item.description)
        values.put(COL_ITEM_QUANTITY, item.quantity)
        values.put(COL_ITEM_PRICE, item.price)
        values.put(COL_ITEM_THRESHOLD, item.lowStockThreshold)
        values.put(COL_ITEM_CREATED_AT, currentTime)
        values.put(COL_ITEM_UPDATED_AT, currentTime)

        val itemId = db.insert(TABLE_INVENTORY, null, values)

        if (itemId != -1L) {
            Log.d(TAG, "Inventory item created: " + item.name + " (ID: " + itemId + ")")
        } else {
            Log.e(TAG, "Failed to create inventory item: " + item.name)
        }

        return itemId
    }

    /**
     * READ: Retrieves a single inventory item by its ID.
     *
     * @param itemId The item's database ID
     * @return InventoryItem if found, null otherwise
     */
    fun getInventoryItem(itemId: Long): InventoryItem? {
        val db = this.readableDatabase

        val columns = arrayOf(
            COL_ITEM_ID, COL_ITEM_USER_ID, COL_ITEM_NAME,
            COL_ITEM_DESCRIPTION, COL_ITEM_QUANTITY, COL_ITEM_PRICE,
            COL_ITEM_THRESHOLD, COL_ITEM_CREATED_AT, COL_ITEM_UPDATED_AT
        )

        val selection = COL_ITEM_ID + " = ?"
        val selectionArgs = arrayOf(itemId.toString())

        val cursor = db.query(
            TABLE_INVENTORY, columns, selection, selectionArgs,
            null, null, null
        )

        var item: InventoryItem? = null

        if (cursor.moveToFirst()) {
            item = cursorToInventoryItem(cursor)
        }

        cursor.close()
        return item
    }

    /**
     * READ: Retrieves all inventory items for a specific user.
     * Items are ordered by name alphabetically.
     *
     * @param userId The user's database ID
     * @return List of InventoryItem objects (empty list if none found)
     */
    fun getAllInventoryItems(userId: Long): MutableList<InventoryItem> {
        val items: MutableList<InventoryItem> = ArrayList()
        val db = this.readableDatabase

        val columns = arrayOf(
            COL_ITEM_ID, COL_ITEM_USER_ID, COL_ITEM_NAME,
            COL_ITEM_DESCRIPTION, COL_ITEM_QUANTITY, COL_ITEM_PRICE,
            COL_ITEM_THRESHOLD, COL_ITEM_CREATED_AT, COL_ITEM_UPDATED_AT
        )

        val selection = COL_ITEM_USER_ID + " = ?"
        val selectionArgs = arrayOf(userId.toString())
        val orderBy = COL_ITEM_NAME + " ASC"

        val cursor = db.query(
            TABLE_INVENTORY, columns, selection, selectionArgs,
            null, null, orderBy
        )

        // loop through rows
        while (cursor.moveToNext()) {
            items.add(cursorToInventoryItem(cursor))
        }

        cursor.close()
        Log.d(TAG, "Retrieved " + items.size + " inventory items for user " + userId)

        return items
    }

    /**
     * READ: Retrieves all low stock items for a specific user.
     * Low stock is defined as quantity <= low_stock_threshold.
     *
     * @param userId The user's database ID
     * @return List of low stock InventoryItem objects
     */
    fun getLowStockItems(userId: Long): MutableList<InventoryItem> {
        val items: MutableList<InventoryItem> = ArrayList()
        val db = this.readableDatabase

        // search where quantity is less than or equal to threshold
        val query = "SELECT * FROM " + TABLE_INVENTORY +
                " WHERE " + COL_ITEM_USER_ID + " = ? AND " +
                COL_ITEM_QUANTITY + " <= " + COL_ITEM_THRESHOLD +
                " ORDER BY " + COL_ITEM_QUANTITY + " ASC"

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        while (cursor.moveToNext()) {
            items.add(cursorToInventoryItem(cursor))
        }

        cursor.close()
        Log.d(TAG, "Found " + items.size + " low stock items for user " + userId)

        return items
    }

    /**
     * UPDATE: Updates an existing inventory item in the database.
     * Updates all fields except id, userId, and createdAt.
     *
     * @param item The InventoryItem with updated values
     * @return true if update successful, false otherwise
     */
    fun updateInventoryItem(item: InventoryItem): Boolean {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_ITEM_NAME, item.name)
        values.put(COL_ITEM_DESCRIPTION, item.description)
        values.put(COL_ITEM_QUANTITY, item.quantity)
        values.put(COL_ITEM_PRICE, item.price)
        values.put(COL_ITEM_THRESHOLD, item.lowStockThreshold)
        values.put(COL_ITEM_UPDATED_AT, System.currentTimeMillis())

        val rowsAffected = db.update(
            TABLE_INVENTORY, values,
            COL_ITEM_ID + " = ?",
            arrayOf(item.id.toString())
        )

        if (rowsAffected > 0) {
            Log.d(TAG, "Inventory item updated: " + item.name + " (ID: " + item.id + ")")
        } else {
            Log.w(TAG, "No inventory item found with ID: " + item.id)
        }

        return rowsAffected > 0
    }

    /**
     * UPDATE: Updates only the quantity of an inventory item.
     * Useful for quick stock adjustments.
     *
     * @param itemId The item's database ID
     * @param newQuantity The new quantity value
     * @return true if update successful, false otherwise
     */
    fun updateItemQuantity(itemId: Long, newQuantity: Int): Boolean {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_ITEM_QUANTITY, newQuantity)
        values.put(COL_ITEM_UPDATED_AT, System.currentTimeMillis())

        val rowsAffected = db.update(
            TABLE_INVENTORY, values,
            COL_ITEM_ID + " = ?",
            arrayOf(itemId.toString())
        )

        Log.d(TAG, "Updated quantity for item " + itemId + " to " + newQuantity)
        return rowsAffected > 0
    }

    /**
     * DELETE: Removes an inventory item from the database.
     *
     * @param itemId The item's database ID
     * @return true if deletion successful, false otherwise
     */
    fun deleteInventoryItem(itemId: Long): Boolean {
        val db = this.writableDatabase

        val rowsDeleted = db.delete(
            TABLE_INVENTORY,
            COL_ITEM_ID + " = ?",
            arrayOf(itemId.toString())
        )

        if (rowsDeleted > 0) {
            Log.d(TAG, "Inventory item deleted (ID: " + itemId + ")")
        } else {
            Log.w(TAG, "No inventory item found to delete with ID: " + itemId)
        }

        return rowsDeleted > 0
    }

    /**
     * DELETE: Removes all inventory items for a specific user.
     * Use with caution - this is irreversible.
     *
     * @param userId The user's database ID
     * @return Number of items deleted
     */
    fun deleteAllUserItems(userId: Long): Int {
        val db = this.writableDatabase

        val rowsDeleted = db.delete(
            TABLE_INVENTORY,
            COL_ITEM_USER_ID + " = ?",
            arrayOf(userId.toString())
        )

        Log.d(TAG, "Deleted " + rowsDeleted + " items for user " + userId)
        return rowsDeleted
    }

    /**
     * Counts the total number of inventory items for a user.
     *
     * @param userId The user's database ID
     * @return Total count of inventory items
     */
    fun getItemCount(userId: Long): Int {
        val db = this.readableDatabase

        val query = "SELECT COUNT(*) FROM " + TABLE_INVENTORY +
                " WHERE " + COL_ITEM_USER_ID + " = ?"

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }

        cursor.close()
        return count
    }

    /**
     * Counts the number of low stock items for a user.
     *
     * @param userId The user's database ID
     * @return Count of low stock items
     */
    fun getLowStockCount(userId: Long): Int {
        val db = this.readableDatabase

        val query = "SELECT COUNT(*) FROM " + TABLE_INVENTORY +
                " WHERE " + COL_ITEM_USER_ID + " = ? AND " +
                COL_ITEM_QUANTITY + " <= " + COL_ITEM_THRESHOLD

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }

        cursor.close()
        return count
    }

    /**
     * Converts a database cursor to an InventoryItem object.
     * Helper method to avoid code duplication.
     *
     * @param cursor Cursor positioned at an inventory item row
     * @return InventoryItem object populated with cursor data
     */
    private fun cursorToInventoryItem(cursor: Cursor): InventoryItem {
        return InventoryItem(
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_ID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_USER_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_DESCRIPTION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_QUANTITY)),
            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_PRICE)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_THRESHOLD)),
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_CREATED_AT)),
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_UPDATED_AT))
        )
    }

    companion object {
        // ==================== Constants ====================
        /** Tag for logging - helps filter log messages in Logcat */
        private const val TAG = "DatabaseHelper"

        /** Database file name, stored in app's private directory */
        private const val DATABASE_NAME = "inventory_manager.db"

        /**
         * Database version, increment this when schema changes.
         * onUpgrade() will be called when version increases.
         */
        private const val DATABASE_VERSION = 1

        // ==================== Table Names ====================
        /** Users table, stores authentication and preferences */
        private const val TABLE_USERS = "users"

        /** Inventory items table, stores inventory data */
        private const val TABLE_INVENTORY = "inventory_items"

        // ==================== Users Table Columns ====================
        private const val COL_USER_ID = "id"
        private const val COL_USER_USERNAME = "username"
        private const val COL_USER_PASSWORD = "password_hash"
        private const val COL_USER_PHONE = "phone_number"
        private const val COL_USER_SMS_ENABLED = "sms_enabled"
        private const val COL_USER_LOW_STOCK_ALERTS = "low_stock_alerts"
        private const val COL_USER_EVENT_ALERTS = "event_alerts"
        private const val COL_USER_GOAL_ALERTS = "goal_alerts"
        private const val COL_USER_CREATED_AT = "created_at"
        private const val COL_USER_LAST_LOGIN = "last_login_at"

        // ==================== Inventory Table Columns ====================
        private const val COL_ITEM_ID = "id"
        private const val COL_ITEM_USER_ID = "user_id"
        private const val COL_ITEM_NAME = "name"
        private const val COL_ITEM_DESCRIPTION = "description"
        private const val COL_ITEM_QUANTITY = "quantity"
        private const val COL_ITEM_PRICE = "price"
        private const val COL_ITEM_THRESHOLD = "low_stock_threshold"
        private const val COL_ITEM_CREATED_AT = "created_at"
        private const val COL_ITEM_UPDATED_AT = "updated_at"

        // ==================== Singleton Instance ====================
        /** Single instance of DatabaseHelper */
        private var instance: DatabaseHelper? = null

        /**
         * Gets the singleton instance of DatabaseHelper.
         * Uses application context to prevent memory leaks.
         * @param context Any context (will be converted to application context)
         * @return The singleton DatabaseHelper instance
         */
        @Synchronized
        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                // application context to help avoid memory leaks
                instance = DatabaseHelper(context.applicationContext)
            }
            return instance!!
        }
    }
}