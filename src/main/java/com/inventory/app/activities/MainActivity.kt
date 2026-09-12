package com.inventory.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.inventory.app.R
import com.inventory.app.adapters.InventoryAdapter
import com.inventory.app.database.DatabaseHelper
import com.inventory.app.models.InventoryItem
import com.inventory.app.utils.SessionManager
import com.inventory.app.utils.SmsHelper

/**
 * MainActivity.kt - Main inventory management screen with data grid.
 *
 * This activity provides full CRUD functionality:
 * - CREATE: Add new inventory items via dialog
 * - READ: Display all items in a RecyclerView grid
 * - UPDATE: Edit existing items (quantity, price, etc.)
 * - DELETE: Remove items with confirmation dialog
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class MainActivity : AppCompatActivity(), InventoryAdapter.OnItemActionListener {

    // ==================== UI Comps ====================

    private lateinit var toolbar: Toolbar
    private lateinit var btnNotifications: ImageButton
    private lateinit var tvTotalItems: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var rvInventory: RecyclerView
    private lateinit var emptyStateView: LinearLayout
    private lateinit var fabAddItem: ExtendedFloatingActionButton

    // ==================== Data Comps ====================

    /** Adapter for displaying inventory items in RecyclerView */
    private lateinit var adapter: InventoryAdapter

    /** List of inventory items - loaded from database */
    private val inventoryItems: MutableList<InventoryItem> = mutableListOf()

    /** Database helper for CRUD operations */
    private lateinit var databaseHelper: DatabaseHelper

    /** Current logged-in user's ID */
    private var currentUserId: Long = 0

    // ==================== Lifecycle Methods ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // current user ID from session
        currentUserId = SessionManager.getCurrentUserId(this)

        // verify user is logged in
        if (currentUserId == SessionManager.INVALID_USER_ID) {
            // no valid session
            redirectToLogin()
            return
        }

        // init db helper
        databaseHelper = DatabaseHelper.getInstance(this)

        // init views
        initializeViews()

        // toolbar with menu
        setupToolbar()

        // RecyclerView for data grid
        setupRecyclerView()

        // click listeners
        setupClickListeners()

        // load inventory data from db and watch fallout
        loadInventoryData()
    }

    override fun onResume() {
        super.onResume()
        // refresh data when returning to this screen
        loadInventoryData()
    }

    // ==================== Initialization Methods ====================

    /**
     * Initialize all view references from the layout.
     */
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        btnNotifications = findViewById(R.id.btnNotifications)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tvLowStock = findViewById(R.id.tvLowStock)
        rvInventory = findViewById(R.id.rvInventory)
        emptyStateView = findViewById(R.id.emptyStateView)
        fabAddItem = findViewById(R.id.fabAddItem)
    }

    /**
     * Setup the toolbar as the action bar.
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        // subtitle with username for personalization
        val username = SessionManager.getCurrentUsername(this)
        if (username != null) {
            supportActionBar?.subtitle = "Welcome, $username"
        }
    }

    /**
     * Setup the RecyclerView with adapter and layout manager.
     * Uses LinearLayoutManager for vertical scrolling list.
     */
    private fun setupRecyclerView() {
        // adapter with click listener reference
        adapter = InventoryAdapter(inventoryItems, this)

        // config RecyclerView
        rvInventory.layoutManager = LinearLayoutManager(this)
        rvInventory.adapter = adapter

        rvInventory.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    /**
     * Setup click listeners for interactive elements.
     */
    private fun setupClickListeners() {
        // notif bell - opens SMS settings screen
        btnNotifications.setOnClickListener {
            startActivity(Intent(this, SmsSettingsActivity::class.java))
        }

        fabAddItem.setOnClickListener { showAddItemDialog() }
    }

    // ==================== Menu Methods ====================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_settings -> {
                // SMS settings
                startActivity(Intent(this, SmsSettingsActivity::class.java))
                true
            }
            R.id.action_check_stock -> {
                // trigger low stock check
                checkLowStockAndNotify()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ==================== Data Operations (CRUD) ====================

    /**
     * READ: Loads all inventory items from the database.
     * Updates the RecyclerView and summary cards.
     */
    private fun loadInventoryData() {
        // clear current list
        inventoryItems.clear()

        // load items from db for user
        inventoryItems.addAll(databaseHelper.getAllInventoryItems(currentUserId))

        // adapter of data change
        adapter.notifyDataSetChanged()

        // summary cards
        updateSummaryCards()

        // empty state
        updateEmptyState()
    }

    /**
     * CREATE: Shows dialog to add a new inventory item.
     * Collects item details and saves to database.
     */
    private fun showAddItemDialog() {
        // show the dialog layout
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_item, null)

        // references to dialog input fields
        val tilItemName = dialogView.findViewById<TextInputLayout>(R.id.tilItemName)
        val tilQuantity = dialogView.findViewById<TextInputLayout>(R.id.tilQuantity)
        val tilPrice = dialogView.findViewById<TextInputLayout>(R.id.tilPrice)

        val etItemName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etItemDescription = dialogView.findViewById<TextInputEditText>(R.id.etItemDescription)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etLowStockThreshold = dialogView.findViewById<TextInputEditText>(R.id.etLowStockThreshold)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        // create and config dialog
        val dialog = MaterialAlertDialogBuilder(this, R.style.AppDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // canc btn dismisses dialog
        btnCancel.setOnClickListener { dialog.dismiss() }

        // save btn validates and saves item
        btnSave.setOnClickListener {
            tilItemName.error = null
            tilQuantity.error = null
            tilPrice.error = null

            // input values
            val name = getTextFromInput(etItemName)
            val description = getTextFromInput(etItemDescription)
            val quantityStr = getTextFromInput(etQuantity)
            val priceStr = getTextFromInput(etPrice)
            val thresholdStr = getTextFromInput(etLowStockThreshold)

            // check required fields
            var isValid = true

            if (name.isEmpty()) {
                tilItemName.error = "Item name is required"
                isValid = false
            }

            if (quantityStr.isEmpty()) {
                tilQuantity.error = "Quantity is required"
                isValid = false
            }

            if (priceStr.isEmpty()) {
                tilPrice.error = "Price is required"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            try {
                // parse num values
                val quantity = quantityStr.toInt()
                val price = priceStr.toDouble()
                val threshold = if (thresholdStr.isEmpty()) 5 else thresholdStr.toInt()

                // new inventory item - assign properties directly
                val newItem = InventoryItem()
                newItem.userId = currentUserId
                newItem.name = name
                newItem.description = description
                newItem.quantity = quantity
                newItem.price = price
                newItem.lowStockThreshold = threshold

                // save to db
                val itemId = databaseHelper.addInventoryItem(newItem)

                if (itemId != -1L) {
                    // success
                    newItem.id = itemId
                    Toast.makeText(this, R.string.item_added, Toast.LENGTH_SHORT).show()

                    // reload data to reflect changes
                    loadInventoryData()

                    // check if new item triggers low stock alert
                    if (newItem.isLowStock) {
                        checkLowStockAndNotify()
                    }
                } else {
                    Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()

            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * UPDATE: Handles item row click - opens the edit dialog.
     *
     * @param position Position of item in the list
     */
    override fun onItemClick(position: Int) {
        if (position < 0 || position >= inventoryItems.size) return
        showEditItemDialog(inventoryItems[position])
    }

    /**
     * Shows edit dialog for an existing item.
     *
     * @param item The item to edit
     */
    private fun showEditItemDialog(item: InventoryItem) {
        // show the dialog layout
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_item, null)

        // references to dialog fields
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tilItemName = dialogView.findViewById<TextInputLayout>(R.id.tilItemName)
        val tilQuantity = dialogView.findViewById<TextInputLayout>(R.id.tilQuantity)
        val tilPrice = dialogView.findViewById<TextInputLayout>(R.id.tilPrice)

        val etItemName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etItemDescription = dialogView.findViewById<TextInputEditText>(R.id.etItemDescription)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etLowStockThreshold = dialogView.findViewById<TextInputEditText>(R.id.etLowStockThreshold)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        // change dialog title to "Edit Item"
        tvDialogTitle.setText(R.string.edit_item)

        // prefill fields with current item values
        etItemName.setText(item.name)
        etItemDescription.setText(item.description)
        etQuantity.setText(item.quantity.toString())
        etPrice.setText(item.price.toString())
        etLowStockThreshold.setText(item.lowStockThreshold.toString())

        // dialog
        val dialog = MaterialAlertDialogBuilder(this, R.style.AppDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // canc btn
        btnCancel.setOnClickListener { dialog.dismiss() }

        // save btn
        btnSave.setOnClickListener {
            tilItemName.error = null
            tilQuantity.error = null
            tilPrice.error = null

            // input values
            val name = getTextFromInput(etItemName)
            val description = getTextFromInput(etItemDescription)
            val quantityStr = getTextFromInput(etQuantity)
            val priceStr = getTextFromInput(etPrice)
            val thresholdStr = getTextFromInput(etLowStockThreshold)

            // check
            var isValid = true

            if (name.isEmpty()) {
                tilItemName.error = "Item name is required"
                isValid = false
            }

            if (quantityStr.isEmpty()) {
                tilQuantity.error = "Quantity is required"
                isValid = false
            }

            if (priceStr.isEmpty()) {
                tilPrice.error = "Price is required"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            try {
                // parse values
                val quantity = quantityStr.toInt()
                val price = priceStr.toDouble()
                val threshold = if (thresholdStr.isEmpty()) 5 else thresholdStr.toInt()

                // check if item became low stock
                val wasLowStock = item.isLowStock

                // update item object
                item.name = name
                item.description = description
                item.quantity = quantity
                item.price = price
                item.lowStockThreshold = threshold

                // update in db
                val success = databaseHelper.updateInventoryItem(item)

                if (success) {
                    Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                    loadInventoryData()

                    // check if update caused low stock condition
                    if (!wasLowStock && item.isLowStock) {
                        checkLowStockAndNotify()
                    }
                } else {
                    Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()

            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * DELETE: Handles delete button click on an inventory item row.
     * Shows confirmation dialog before deleting.
     *
     * @param position Position of item in the list
     */
    override fun onDeleteClick(position: Int) {
        if (position < 0 || position >= inventoryItems.size) return

        val item = inventoryItems[position]

        // confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(getString(R.string.delete_confirmation_message, item.name))
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                // del from db
                val success = databaseHelper.deleteInventoryItem(item.id)

                if (success) {
                    // remove from list and update UI
                    inventoryItems.removeAt(position)
                    adapter.notifyItemRemoved(position)

                    // update summary cards
                    updateSummaryCards()
                    updateEmptyState()

                    Toast.makeText(this, R.string.item_deleted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ==================== UI Update Methods ====================

    /**
     * Updates the summary cards with current counts.
     * Called after any data change.
     */
    private fun updateSummaryCards() {
        // get counts from db for accuracy
        val totalItems = databaseHelper.getItemCount(currentUserId)
        val lowStockCount = databaseHelper.getLowStockCount(currentUserId)

        // update UI
        tvTotalItems.text = totalItems.toString()
        tvLowStock.text = lowStockCount.toString()
    }

    /**
     * Shows or hides the empty state view based on item count.
     */
    private fun updateEmptyState() {
        if (inventoryItems.isEmpty()) {
            rvInventory.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
        } else {
            rvInventory.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    // ==================== SMS Notification Methods ====================

    /**
     * Checks for low stock items and sends SMS notification if applicable.
     * Called when items are added/updated or manually triggered.
     */
    private fun checkLowStockAndNotify() {
        // SmsHelper to check and notify
        SmsHelper.checkAndNotifyLowStock(this, currentUserId)

        // feedback to user
        val lowStockCount = databaseHelper.getLowStockCount(currentUserId)
        if (lowStockCount > 0 && SmsHelper.hasSmsPermission(this)) {
            Toast.makeText(
                this,
                "$lowStockCount item(s) are low on stock",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==================== Session Methods ====================

    /**
     * Performs logout operation.
     * Clears session and redirects to login screen.
     */
    private fun performLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout)
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton(R.string.logout) { _, _ ->
                SessionManager.logout(this)

                // to login
                redirectToLogin()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Redirects to the login screen.
     * Clears the activity stack to prevent back navigation.
     */
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // ==================== Helper Methods ====================

    /**
     * Gets trimmed text from an EditText field.
     *
     * @param editText The input field
     * @return Trimmed string or empty string if null
     */
    private fun getTextFromInput(editText: TextInputEditText): String {
        return editText.text?.toString()?.trim() ?: ""
    }
}