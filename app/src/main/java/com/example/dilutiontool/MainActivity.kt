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
import android.widget.CheckBox
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
    var isProgrammaticChange = false // Flag per sapere se il cambiamento è programmatico

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

        dilutionRatioEditText = findViewById(R.id.dilutionRatioEditText)
        val totalLiquidEditText = findViewById<EditText>(R.id.totalLiquidEditText)
        val resultText = findViewById<EditText>(R.id.resultText)
        val waterResultText = findViewById<EditText>(R.id.waterResultText)
        productContainer = findViewById(R.id.productContainer)
        selectedProductNameTextView = findViewById(R.id.selectedProductName)
        selectedProductDescriptionTextView = findViewById(R.id.selectedProductDescription)
        selectedProductImageView = findViewById(R.id.selectedProductImage)
        selectedProductLinkTextView = findViewById(R.id.selectedProductLinkTextView)

        // Associa ogni CheckBox al relativo EditText in una mappa
        val checkBoxEditTextMap = mapOf(
            findViewById<CheckBox>(R.id.totalLiquidLockCheckBox) to findViewById<EditText>(R.id.totalLiquidEditText),
            findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox) to findViewById<EditText>(R.id.dilutionRatioEditText),
            findViewById<CheckBox>(R.id.waterResultTextLockCheckBox) to findViewById<EditText>(R.id.waterResultText),
            findViewById<CheckBox>(R.id.resultTextLockCheckBox) to findViewById<EditText>(R.id.resultText)
        )
        val checkBoxes = listOf(
            findViewById<CheckBox>(R.id.totalLiquidLockCheckBox),
            findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox),
            findViewById<CheckBox>(R.id.waterResultTextLockCheckBox),
            findViewById<CheckBox>(R.id.resultTextLockCheckBox)
        )

        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Se ci sono più di 2 CheckBox selezionate, disabilita le altre CheckBox non selezionate
                if (checkBoxes.count { it.isChecked } >= 2) {
                    checkBoxes.forEach { otherCheckBox ->
                        if (!otherCheckBox.isChecked) {
                            otherCheckBox.isEnabled = false
                        }
                    }
                } else { // Riabilita tutte le CheckBox non selezionate se una CheckBox è deselezionata
                    checkBoxes.forEach { otherCheckBox ->
                        if (!otherCheckBox.isChecked) {
                            otherCheckBox.isEnabled = true // Riabilita le CheckBox non selezionate
                        }
                    }
                }

                // Imposta isEnabled dell'EditText in base allo stato della CheckBox
                val editText = checkBoxEditTextMap[checkBox]
                editText?.isEnabled = isChecked
            }
        }

        productContainer.setOnClickListener { launchProductListActivity() }

        fun calculateResult() {
            var totalLiquid = totalLiquidEditText.text.toString().toDoubleOrNull() ?: 0.0
            var dilutionRatio = dilutionRatioEditText.text.toString().toIntOrNull() ?: 0
            var water = waterResultText.text.toString().toDoubleOrNull() ?: 0.0
            var concentrate = resultText.text.toString().toDoubleOrNull() ?: 0.0

            if (totalLiquidEditText.isEnabled && dilutionRatioEditText.isEnabled) {
                concentrate = totalLiquid / (dilutionRatio + 1)
                water = totalLiquid - concentrate
                changeTextProgrammatically(resultText, formatNumber(concentrate))
                changeTextProgrammatically(waterResultText, formatNumber(water))
            } else if (totalLiquidEditText.isEnabled && waterResultText.isEnabled) {
                // TODO
            } else if (totalLiquidEditText.isEnabled && resultText.isEnabled) {
                // TODO
            } else if (dilutionRatioEditText.isEnabled && waterResultText.isEnabled) {
                // TODO
            } else if (dilutionRatioEditText.isEnabled && resultText.isEnabled) {
                // TODO
            } else if (resultText.isEnabled && waterResultText.isEnabled) {
                // TODO
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isProgrammaticChange) { // Cambiamento fatto dall'utente
                    calculateResult()
                }

                if (currentFocus == dilutionRatioEditText) {
                    // TODO Valuta cosa fare se il range di diluizione attuale (dilutionRatioEditText.text) non è compreso nel range di diluizione del prodotto selezionato
                }
            }
        }

        dilutionRatioEditText.addTextChangedListener(textWatcher)
        totalLiquidEditText.addTextChangedListener(textWatcher)
        waterResultText.addTextChangedListener(textWatcher)
        resultText.addTextChangedListener(textWatcher)
    }

    // Funzione per formattare i numeri
    private fun formatNumber(value: Double): String {
        return if (value == value.toInt().toDouble()) {
            // Se il numero è "intero" (senza decimali significativi)
            value.toInt().toString()  // Rimuove i decimali
        } else {
            // Se il numero è decimale
            String.format("%.1f", value)  // Mostra un solo decimale
        }
    }

    // Cambiamento programmatico
    private fun changeTextProgrammatically(editText: EditText, newText: String) {
        isProgrammaticChange = true
        editText.setText(newText)
        isProgrammaticChange = false
    }
}
