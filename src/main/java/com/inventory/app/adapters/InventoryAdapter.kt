package com.inventory.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inventory.app.R
import com.inventory.app.models.InventoryItem

/**
 * InventoryAdapter.kt - RecyclerView adapter for displaying inventory items.
 *
 * This adapter displays inventory items in a list format with:
 * - Item name and description
 * - Current quantity with low stock indicator
 * - Formatted price
 * - Delete button for each row
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class InventoryAdapter(
    private val items: MutableList<InventoryItem>,
    private val listener: OnItemActionListener?
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    // ==================== Interface ====================

    /**
     * Interface for handling item actions.
     * Implemented by the hosting Activity to respond to user interactions.
     */
    interface OnItemActionListener {
        /**
         * Called when a row is clicked. Used to open the edit dialog.
         *
         * @param position Position of clicked item in the list
         */
        fun onItemClick(position: Int)

        /**
         * Called when the delete button is clicked.
         *
         * @param position Position of item to delete
         */
        fun onDeleteClick(position: Int)
    }

    // ==================== Adapter Methods ====================

    /**
     * Creates a new ViewHolder when the RecyclerView needs a new item view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_row, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data to a ViewHolder when it needs to display an item.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    /**
     * Returns the total number of items in the data list.
     */
    override fun getItemCount(): Int = items.size

    // ==================== ViewHolder Class ====================

    /**
     * ViewHolder class for inventory item rows.
     * Holds references to views within the item layout and binds data to them.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvLowStockBadge: TextView = itemView.findViewById(R.id.tvLowStockBadge)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnDeleteRow: ImageButton = itemView.findViewById(R.id.btnDeleteRow)

        /**
         * Binds an InventoryItem's data to the views.
         *
         * @param item The InventoryItem to display
         * @param position Position in the adapter
         */
        fun bind(item: InventoryItem, position: Int) {
            // item name
            tvItemName.text = item.name

            // description
            if (item.hasDescription()) {
                tvItemDescription.text = item.description
                tvItemDescription.visibility = View.VISIBLE
            } else {
                tvItemDescription.visibility = View.GONE
            }

            // quantity
            tvQuantity.text = item.quantity.toString()

            // show/hide low stock badge based on threshold
            tvLowStockBadge.visibility = if (item.isLowStock) View.VISIBLE else View.GONE

            // formatted price (e.g., "$99.99")
            tvPrice.text = item.formattedPrice

            // delete button
            btnDeleteRow.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(adapterPosition)
                }
            }

            // row click listener for editing
            itemView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onItemClick(adapterPosition)
                }
            }
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Updates the data list and refreshes the display.
     *
     * @param newItems New list of items to display
     */
    fun updateItems(newItems: List<InventoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * Adds a single item to the list.
     *
     * @param item Item to add
     */
    fun addItem(item: InventoryItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    /**
     * Removes an item at the specified position.
     *
     * @param position Position of item to remove
     */
    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Updates an item at the specified position.
     *
     * @param position Position of item to update
     * @param item Updated item data
     */
    fun updateItem(position: Int, item: InventoryItem) {
        if (position in items.indices) {
            items[position] = item
            notifyItemChanged(position)
        }
    }
}