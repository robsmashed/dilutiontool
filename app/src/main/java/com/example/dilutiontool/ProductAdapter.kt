package com.example.dilutiontool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dilutiontool.entity.ProductWithDilutions

class ProductAdapter(
    private val context: Context,
    private var productsWithDilutions: List<ProductWithDilutions>,
    private val onItemClick: (ProductWithDilutions) -> Unit,
    private val onSelectionChange: (MutableSet<ProductWithDilutions>) -> Unit,
    private val onItemDelete: (ProductWithDilutions) -> Unit,
    private val onItemEdit: (ProductWithDilutions) -> Unit,
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<ProductWithDilutions>()

    fun updateList(filteredProducts: List<ProductWithDilutions>) {
        productsWithDilutions = filteredProducts
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) selectedItems.clear()  // Reset selezione se disabilitato
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productsWithDilutions[position]
        holder.bind(product, isSelectionMode, selectedItems.contains(product))
    }

    override fun getItemCount(): Int = productsWithDilutions.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(R.id.productName)
        private val productDescriptionTextView: TextView = itemView.findViewById(R.id.productDescription)
        private val productImageView: ImageView = itemView.findViewById(R.id.productImage)
        private val productLinkTextView: TextView = itemView.findViewById(R.id.productLinkTextView)
        private val productCheckbox: CheckBox = itemView.findViewById(R.id.productCheckbox)
        private val productMoreButton: ImageButton = itemView.findViewById(R.id.itemOptionsMenu)

        fun bind(productWithDilution: ProductWithDilutions, isSelectionMode: Boolean, isSelected: Boolean) {
            productNameTextView.text = productWithDilution.product.name
            productDescriptionTextView.text = productWithDilution.product.description

            val spannableString = SpannableString("Link prodotto")
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(productWithDilution.product.link))
                    context.startActivity(intent)
                }
            }
            spannableString.setSpan(clickableSpan, 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            productLinkTextView.text = spannableString
            productLinkTextView.movementMethod = if (isSelectionMode) null else LinkMovementMethod.getInstance()

            Glide.with(itemView.context)
                .load(productWithDilution.product.image)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(productImageView)

            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(productWithDilution)
                } else {
                    onItemClick(productWithDilution)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    setSelectionMode(true)
                }
                toggleSelection(productWithDilution)
                true
            }

            productCheckbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            productCheckbox.isChecked = isSelected

            if (productWithDilution.product.link.isNullOrBlank()) {
                productLinkTextView.visibility = View.INVISIBLE
            }

            // add roduct item edit/delete menu
            productMoreButton.setOnClickListener { view ->
                val popupMenu = PopupMenu(context, view)
                val inflater = popupMenu.menuInflater
                inflater.inflate(R.menu.menu_more, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onItemEdit(productWithDilution)
                            true
                        }
                        R.id.action_delete -> {
                            onItemDelete(productWithDilution)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }

        private fun toggleSelection(product: ProductWithDilutions) {
            if (selectedItems.contains(product)) {
                selectedItems.remove(product)
            } else {
                selectedItems.add(product)
            }

            notifyItemChanged(adapterPosition)

            // Se nessun elemento è selezionato, esci dalla modalità selezione
            if (selectedItems.isEmpty()) {
                setSelectionMode(false)
            }

            onSelectionChange(selectedItems) // Notifica l'Activity
        }
    }
}
