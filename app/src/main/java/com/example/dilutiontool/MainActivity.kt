package com.example.dilutiontool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions

class MainActivity : AppCompatActivity() {
    private lateinit var dilutionRatioEditText: EditText
    private lateinit var productContainer: LinearLayout
    private lateinit var selectedProductNameTextView: TextView
    private lateinit var selectedProductDescriptionTextView: TextView
    private lateinit var selectedProductImageView: ImageView
    private lateinit var selectedProductLinkTextView: TextView

    // Dichiarazione di ActivityResultLauncher
    private val productSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDilution = result.data?.getParcelableExtra<Dilution>("selectedDilution")
            val selectedProductWithDilutions = result.data?.getParcelableExtra<ProductWithDilutions>("selectedProductWithDilutions")
            setSelectedProduct(selectedProductWithDilutions, selectedDilution);
        }
    }

    private fun setSelectedProduct(selectedProductWithDilutions: ProductWithDilutions?, selectedDilution: Dilution?) {
        if (selectedProductWithDilutions != null && selectedDilution != null) {
            dilutionRatioEditText.setText(selectedDilution.value.toString())

            selectedProductNameTextView.text = selectedProductWithDilutions.product.name
            selectedProductDescriptionTextView.text = getDescription(selectedDilution)

            val spannableString = SpannableString("Link prodotto")
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedProductWithDilutions.product.link))
                    startActivity(intent)
                }
            }
            spannableString.setSpan(
                clickableSpan,
                0,
                spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            selectedProductLinkTextView.text = spannableString
            selectedProductLinkTextView.movementMethod = LinkMovementMethod.getInstance()

            if (selectedProductWithDilutions.product.link.isBlank()) {
                selectedProductLinkTextView.visibility = View.INVISIBLE
            }

            // Usa Glide per caricare l'immagine
            Glide.with(this)
                .load(selectedProductWithDilutions.product.imageUrl)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(selectedProductImageView)

            findViewById<LinearLayout>(R.id.selectedProductContainer).visibility = View.VISIBLE
            findViewById<TextView>(R.id.noSelectedProductLabel).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.selectedProductContainer).visibility = View.GONE
            findViewById<TextView>(R.id.noSelectedProductLabel).visibility = View.VISIBLE
        }
    }

    private fun launchProductListActivity() {
        val intent = Intent(this, ProductListActivity::class.java)
        productSelectionLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dilutionRatioEditText = findViewById(R.id.dilutionRatio)
        val totalLiquidEditText = findViewById<EditText>(R.id.totalLiquid)
        val resultText = findViewById<TextView>(R.id.resultText)
        val waterResultText = findViewById<TextView>(R.id.waterResultText)
        productContainer = findViewById(R.id.productContainer)
        selectedProductNameTextView = findViewById(R.id.selectedProductName)
        selectedProductDescriptionTextView = findViewById(R.id.selectedProductDescription)
        selectedProductImageView = findViewById(R.id.selectedProductImage)
        selectedProductLinkTextView = findViewById(R.id.selectedProductLinkTextView)

        productContainer.setOnClickListener { launchProductListActivity() }

        // Funzione per calcolare e aggiornare il risultato
        fun calculateResult() {
            val totalLiquid = totalLiquidEditText.text.toString().toDoubleOrNull() ?: 0.0
            val dilutionRatio = dilutionRatioEditText.text.toString().toDoubleOrNull() ?: 0.0

            val concentrate = totalLiquid / (dilutionRatio + 1)
            val water = totalLiquid - concentrate
            val formattedConcentrate = String.format("%.1f", concentrate)
            val formattedWater = String.format("%.1f", water)
            resultText.text = "$formattedConcentrate ml"
            waterResultText.text = "$formattedWater ml"
        }

        // Aggiungi il TextWatcher agli EditText
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateResult()

                if (currentFocus == dilutionRatioEditText) {
                    // Valuta se aggiungere un warning se il range di diluizione attuale (dilutionRatioEditText.text) non è compreso nel range di diluizione del prodotto selezionato
                }
            }
        }

        dilutionRatioEditText.addTextChangedListener(textWatcher)
        totalLiquidEditText.addTextChangedListener(textWatcher)

        calculateResult()
    }
}
