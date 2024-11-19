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

class ProductAdapter(
    private val context: Context,
    private var productsWithDilutions: List<ProductWithDilutions>,
    private val onItemClick: (ProductWithDilutions) -> Unit,
    private val onLongItemClick: (ProductWithDilutions) -> Boolean
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Metodo per aggiornare la lista dei prodotti
    fun updateList(filteredProducts: List<ProductWithDilutions>) {
        productsWithDilutions = filteredProducts
        notifyDataSetChanged()  // Notifica che la lista dei prodotti è cambiata
    }

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

            val spannableString = SpannableString("Link prodotto")
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

            // Gestisci il normale click
            itemView.setOnClickListener {
                onItemClick(productWithDilution) // Chiamato quando il prodotto viene selezionato
            }

            // Gestisci il long click
            itemView.setOnLongClickListener {
                onLongItemClick(productWithDilution) // Chiamato quando il prodotto viene selezionato
            }

            if (productWithDilution.product.link.isBlank()) {
                productLinkTextView.visibility = View.INVISIBLE
            }
        }
    }
}
