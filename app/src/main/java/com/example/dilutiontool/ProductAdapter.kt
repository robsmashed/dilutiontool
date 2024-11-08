package com.example.dilutiontool // Assicurati che il pacchetto sia corretto

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dilutiontool.entity.ProductWithDilutions

// Adapter per il RecyclerView
class ProductAdapter(
    private val context: Context,
    private val productsWithDilutions: List<ProductWithDilutions>,
    private val onItemClick: (ProductWithDilutions) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productsWithDilutions[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = productsWithDilutions.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(R.id.productName)
        private val productDescriptionTextView: TextView = itemView.findViewById(R.id.productDescription)
        private val productImageView: ImageView = itemView.findViewById(R.id.productImage)
        private val productLinkTextView: TextView = itemView.findViewById(R.id.productLinkTextView)

        fun bind(productWithDilution: ProductWithDilutions) {
            productNameTextView.text = productWithDilution.product.name
            productDescriptionTextView.text = productWithDilution.product.description

            val spannableString = SpannableString("Scheda prodotto")
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(productWithDilution.product.link))
                    context.startActivity(intent)
                }
            }
            spannableString.setSpan(
                clickableSpan,
                0,
                spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            productLinkTextView.text = spannableString
            productLinkTextView.movementMethod = LinkMovementMethod.getInstance()

            // Usa Glide per caricare l'immagine
            Glide.with(itemView.context)
                .load(productWithDilution.product.imageUrl)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(productImageView)

            itemView.setOnClickListener {
                onItemClick(productWithDilution) // Chiamato quando il prodotto viene selezionato
            }
        }
    }
}
