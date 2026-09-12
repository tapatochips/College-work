package com.inventory.app.models

import java.text.NumberFormat
import java.util.Locale

/**
 * InventoryItem.kt - Model class representing an item in the inventory.
 *
 * This class encapsulates all data related to a single inventory item.
 * Each item belongs to a specific user (identified by userId) to support
 * multi-user functionality.
 *
 * Database Table: inventory_items
 * - id: Primary key, auto-increment
 * - user_id: Foreign key to users table
 * - name: Item name (required)
 * - description: Optional description
 * - quantity: Current stock quantity
 * - price: Unit price
 * - low_stock_threshold: Alert threshold
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class InventoryItem() {

    // ==================== Properties ====================

    /** Unique identifier for the item (primary key in database) */
    var id: Long = 0

    /** Foreign key linking to the user who owns this item */
    var userId: Long = 0

    /** Name of the inventory item (required field) */
    var name: String? = null
        set(value) {
            field = value
            updatedAt = System.currentTimeMillis()
        }

    /** Optional description providing additional details */
    var description: String? = null
        set(value) {
            field = value
            updatedAt = System.currentTimeMillis()
        }

    /** Current quantity in stock */
    var quantity: Int = 0
        set(value) {
            field = value
            updatedAt = System.currentTimeMillis()
        }

    /** Unit price of the item */
    var price: Double = 0.0
        set(value) {
            field = value
            updatedAt = System.currentTimeMillis()
        }

    /**
     * Threshold below which the item is considered "low stock".
     * Default value is 5 units.
     */
    var lowStockThreshold: Int = 5
        set(value) {
            field = value
            updatedAt = System.currentTimeMillis()
        }

    /** Timestamp when the item was first added to inventory */
    var createdAt: Long = System.currentTimeMillis()

    /** Timestamp of the most recent update to this item */
    var updatedAt: Long = System.currentTimeMillis()

    // ==================== Constructors ====================

    /**
     * Constructor for creating a new inventory item with essential fields.
     *
     * @param name Item name
     * @param quantity Initial stock quantity
     * @param price Unit price
     */
    constructor(name: String?, quantity: Int, price: Double) : this() {
        this.name = name
        this.quantity = quantity
        this.price = price
    }

    /**
     * Constructor for creating an item with all basic details.
     *
     * @param name Item name
     * @param description Optional description
     * @param quantity Initial stock quantity
     * @param price Unit price
     * @param lowStockThreshold Custom low stock threshold
     */
    constructor(
        name: String?, description: String?, quantity: Int,
        price: Double, lowStockThreshold: Int
    ) : this() {
        this.name = name
        this.description = description
        this.quantity = quantity
        this.price = price
        this.lowStockThreshold = lowStockThreshold
    }

    /**
     * Full constructor for loading from database.
     *
     * @param id Database primary key
     * @param userId Owner's user ID
     * @param name Item name
     * @param description Optional description
     * @param quantity Current quantity
     * @param price Unit price
     * @param lowStockThreshold Low stock alert threshold
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     */
    constructor(
        id: Long, userId: Long, name: String?, description: String?,
        quantity: Int, price: Double, lowStockThreshold: Int,
        createdAt: Long, updatedAt: Long
    ) : this() {
        this.id = id
        this.userId = userId
        this.name = name
        this.description = description
        this.quantity = quantity
        this.price = price
        this.lowStockThreshold = lowStockThreshold
        // Assign timestamps last so the property setters above don't overwrite them.
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }

    // ==================== Computed Properties ====================

    /**
     * Checks if the item's current quantity is at or below the low stock threshold.
     * Used to trigger low stock alerts and display warnings in the UI.
     */
    val isLowStock: Boolean
        get() = quantity <= lowStockThreshold

    /**
     * Formats the price as a currency string using US locale.
     * Example: 29.99 becomes "$29.99"
     */
    val formattedPrice: String
        get() = NumberFormat.getCurrencyInstance(Locale.US).format(price)

    /**
     * Calculates the total value of this item (price x quantity).
     */
    val totalValue: Double
        get() = price * quantity

    /**
     * Formats the total value as a currency string.
     */
    val formattedTotalValue: String
        get() = NumberFormat.getCurrencyInstance(Locale.US).format(totalValue)

    // ==================== Utility Methods ====================

    /**
     * Checks if the item has a description.
     *
     * @return true if description is non-null and non-blank
     */
    fun hasDescription(): Boolean {
        return !description.isNullOrBlank()
    }

    /**
     * Updates the item's timestamp to the current time.
     * Should be called before any database update operation.
     */
    fun markAsUpdated() {
        this.updatedAt = System.currentTimeMillis()
    }

    /**
     * String representation for debugging purposes.
     */
    override fun toString(): String {
        return "InventoryItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", isLowStock=" + isLowStock +
                '}'
    }

    /**
     * Equality check based on database ID.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as InventoryItem
        return id == that.id
    }

    /**
     * Hash code based on database ID.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }
}