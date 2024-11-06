package com.example.dilutiontool // Assicurati che il pacchetto sia corretto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Data class per rappresentare un prodotto
data class Product(val id: String, val name: String, val description: String, val dilutions: Array<Dilution>, val imageUrl: String)
data class Dilution(val description: String, val value: Number)

// Adapter per il RecyclerView
class ProductAdapter(private val products: List<Product>, private val onItemClick: (Product) -> Unit) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(R.id.productName)
        private val productDescriptionTextView: TextView = itemView.findViewById(R.id.productDescription)
        private val productImageView: ImageView = itemView.findViewById(R.id.productImage)

        fun bind(product: Product) {
            productNameTextView.text = product.name
            productDescriptionTextView.text = product.description

            // Usa Glide per caricare l'immagine
            Glide.with(itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(productImageView)

            itemView.setOnClickListener {
                onItemClick(product) // Chiamato quando il prodotto viene selezionato
            }
        }
    }
}
