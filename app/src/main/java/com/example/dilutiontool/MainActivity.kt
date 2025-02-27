package com.example.dilutiontool

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.w3c.dom.Text
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var selectedProductDilution: Dilution? = null
    private lateinit var dilutionRatioEditText: EditText
    private lateinit var productContainer: LinearLayout
    private lateinit var selectedProductNameTextView: TextView
    private lateinit var selectedProductDescriptionTextView: TextView
    private lateinit var selectedProductImageView: ImageView
    private lateinit var selectedProductLinkTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var discardProductSelectionFab: FloatingActionButton
    private lateinit var selectedProductContainer: LinearLayout
    private lateinit var noSelectedProductLabel: TextView
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
            discardProductSelectionFab.visibility = View.VISIBLE
            selectedProductDilution = selectedDilution
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

            if (selectedProductWithDilutions.product.link === null || selectedProductWithDilutions.product.link.isBlank()) {
                selectedProductLinkTextView.visibility = View.INVISIBLE
            }

            Glide.with(this)
                .load(selectedProductWithDilutions.product.imageUrl)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(selectedProductImageView)

            // Initialize seekbar
            seekBar.progress = 0
            if (selectedDilution.minValue != selectedDilution.value) {
                seekBar.max = selectedDilution.value - selectedDilution.minValue
                seekBar.visibility = View.VISIBLE
            } else {
                seekBar.visibility = View.GONE
            }

            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        dilutionRatioEditText.setText(getStringValue((selectedDilution.value - progress).toDouble()))
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            selectedProductContainer.visibility = View.VISIBLE
            noSelectedProductLabel.visibility = View.GONE
        } else {
            discardCurrentProductSelection()
        }
    }

    private fun discardCurrentProductSelection() {
        discardProductSelectionFab.visibility = View.GONE
        selectedProductDilution = null
        selectedProductContainer.visibility = View.GONE
        noSelectedProductLabel.visibility = View.VISIBLE
        seekBar.visibility = View.GONE
    }

    private fun launchProductListActivity() {
        val intent = Intent(this, ProductListActivity::class.java)
        productSelectionLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        dilutionRatioEditText = findViewById(R.id.dilutionRatioEditText)
        val totalLiquidEditText = findViewById<EditText>(R.id.totalEditText)
        val concentrateEditText = findViewById<EditText>(R.id.concentrateEditText)
        val waterEditText = findViewById<EditText>(R.id.waterEditText)
        productContainer = findViewById(R.id.productContainer)
        selectedProductNameTextView = findViewById(R.id.selectedProductName)
        selectedProductDescriptionTextView = findViewById(R.id.selectedProductDescription)
        selectedProductImageView = findViewById(R.id.selectedProductImage)
        selectedProductLinkTextView = findViewById(R.id.selectedProductLinkTextView)
        seekBar = findViewById(R.id.seekBar)
        selectedProductContainer = findViewById(R.id.selectedProductContainer)
        noSelectedProductLabel = findViewById(R.id.noSelectedProductLabel)
        discardProductSelectionFab = findViewById(R.id.discardProductSelectionFab)
        discardProductSelectionFab.setOnClickListener {
            discardCurrentProductSelection()
        }
        // Associa ogni CheckBox al relativo EditText in una mappa
        val checkBoxEditTextMap = mapOf(
            findViewById<CheckBox>(R.id.totalLiquidLockCheckBox) to findViewById<EditText>(R.id.totalEditText),
            findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox) to findViewById<EditText>(R.id.dilutionRatioEditText),
            findViewById<CheckBox>(R.id.waterResultTextLockCheckBox) to findViewById<EditText>(R.id.waterEditText),
            findViewById<CheckBox>(R.id.resultTextLockCheckBox) to findViewById<EditText>(R.id.concentrateEditText)
        )
        val checkBoxes = listOf(
            findViewById<CheckBox>(R.id.totalLiquidLockCheckBox),
            findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox),
            findViewById<CheckBox>(R.id.waterResultTextLockCheckBox),
            findViewById<CheckBox>(R.id.resultTextLockCheckBox)
        )

        val editTexts = listOf(dilutionRatioEditText, totalLiquidEditText, waterEditText, concentrateEditText)

        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBoxes.count { it.isChecked } >= 2) {
                    checkBoxes.forEach { otherCheckBox ->
                        if (!otherCheckBox.isChecked) {
                            otherCheckBox.isEnabled = false // Disabilita le altre CheckBox non selezionate
                        } else {
                            checkBoxEditTextMap[otherCheckBox]?.isEnabled = isChecked // Imposta isEnabled dell'EditText in base allo stato della CheckBox
                        }
                    }
                    enableProductSelection(findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox).isChecked)
                } else {
                    checkBoxes.forEach { otherCheckBox ->
                        checkBoxEditTextMap[otherCheckBox]?.isEnabled = false
                        if (!otherCheckBox.isChecked) {
                            otherCheckBox.isEnabled = true // Riabilita le CheckBox non selezionate
                        }
                    }
                    enableProductSelection(false)
                }
            }
        }

        val launchProductList = View.OnClickListener {
            launchProductListActivity()
        }
        selectedProductContainer.setOnClickListener(launchProductList)
        noSelectedProductLabel.setOnClickListener(launchProductList)


        fun calculateResult(currentEditText: EditText) {
            var totalLiquid = getDoubleValue(totalLiquidEditText)
            var dilutionRatio = getDoubleValue(dilutionRatioEditText)
            var water = getDoubleValue(waterEditText)
            var concentrate = getDoubleValue(concentrateEditText)

            if (totalLiquidEditText.isEnabled && dilutionRatioEditText.isEnabled) {
                concentrate = totalLiquid / (dilutionRatio + 1)
                water = totalLiquid - concentrate
                changeTextProgrammatically(concentrateEditText, getStringValue(concentrate))
                changeTextProgrammatically(waterEditText, getStringValue(water))
            } else if (totalLiquidEditText.isEnabled && waterEditText.isEnabled) {
                if (totalLiquid - water < 0) {
                    if (currentEditText !== waterEditText) {
                        water = totalLiquid
                        changeTextProgrammatically(waterEditText, getStringValue(totalLiquid))
                    } else if (currentEditText !== totalLiquidEditText) {
                        totalLiquid = water
                        changeTextProgrammatically(totalLiquidEditText, getStringValue(water))
                    }
                }

                concentrate = totalLiquid - water

                dilutionRatio = if (concentrate == totalLiquid) {
                    0.0
                } else if (concentrate == 0.0) {
                    Double.POSITIVE_INFINITY
                } else {
                    water / concentrate
                }
                changeTextProgrammatically(concentrateEditText, getStringValue(concentrate))
                changeTextProgrammatically(dilutionRatioEditText, getStringValue(dilutionRatio))
            } else if (totalLiquidEditText.isEnabled && concentrateEditText.isEnabled) {
                if (totalLiquid - concentrate < 0) {
                    if (currentEditText !== totalLiquidEditText) {
                        totalLiquid = concentrate
                        changeTextProgrammatically(totalLiquidEditText, getStringValue(concentrate))
                    } else if (currentEditText !== concentrateEditText) {
                        concentrate = totalLiquid
                        changeTextProgrammatically(concentrateEditText, getStringValue(totalLiquid))
                    }
                }

                water = totalLiquid - concentrate
                dilutionRatio = if (totalLiquid == 0.0 && concentrate == 0.0) 0.0 else totalLiquid / concentrate - 1
                changeTextProgrammatically(waterEditText, getStringValue(water))
                changeTextProgrammatically(dilutionRatioEditText, getStringValue(dilutionRatio))
            } else if (dilutionRatioEditText.isEnabled && waterEditText.isEnabled) {
                if (dilutionRatio == 0.0) {
                    if (currentEditText !== waterEditText) {
                        water = 0.0
                        changeTextProgrammatically(waterEditText, getStringValue(water))
                        concentrate = totalLiquid;
                        changeTextProgrammatically(concentrateEditText, getStringValue(totalLiquid))
                    } else if (currentEditText !== dilutionRatioEditText) {
                        water = 0.0
                        changeTextProgrammatically(waterEditText, getStringValue(water))
                        waterEditText.selectAll()
                        flashView(dilutionRatioEditText)
                    }
                } else {
                    totalLiquid = (water / dilutionRatio) + water
                    concentrate = totalLiquid - water
                    changeTextProgrammatically(totalLiquidEditText, getStringValue(totalLiquid))
                    changeTextProgrammatically(concentrateEditText, getStringValue(concentrate))
                }
            } else if (dilutionRatioEditText.isEnabled && concentrateEditText.isEnabled) {
                if (dilutionRatio == Double.POSITIVE_INFINITY && currentEditText !== dilutionRatioEditText) {
                    concentrate = 0.0
                    changeTextProgrammatically(concentrateEditText, getStringValue(concentrate))
                    concentrateEditText.selectAll()
                    flashView(dilutionRatioEditText)
                } else {
                    totalLiquid = concentrate * (dilutionRatio + 1)
                    water = totalLiquid - concentrate
                    changeTextProgrammatically(totalLiquidEditText, getStringValue(totalLiquid))
                    changeTextProgrammatically(waterEditText, getStringValue(water))
                }
            } else if (concentrateEditText.isEnabled && waterEditText.isEnabled) {
                totalLiquid = concentrate + water
                dilutionRatio = water / concentrate

                if (dilutionRatio.isNaN()) {
                    dilutionRatio = 0.0
                }

                changeTextProgrammatically(totalLiquidEditText, getStringValue(totalLiquid))
                changeTextProgrammatically(dilutionRatioEditText, getStringValue(dilutionRatio))
            }
        }

        editTexts.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateSeekbar(editText)
                    if (!isProgrammaticChange) { // Cambiamento fatto dall'utente
                        calculateResult(editText)
                    }
                }
            })
            editText.setOnFocusChangeListener { _, hasFocus ->
                // Se l'EditText ha il focus, seleziona tutto il testo al suo interno
                if (hasFocus) {
                    editText.post {
                        editText.selectAll()
                    }
                }
            }
        }

        calculateResult(totalLiquidEditText) // inizializzazione a partire dai due valori iniziali
    }

    private fun enableProductSelection(enable: Boolean) {
        productContainer.alpha = if (enable) 1.0f else 0.5f // Imposta l'alpha per dare l'effetto di disabilitazione
        findViewById<LinearLayout>(R.id.selectedProductContainer).isClickable = enable
        findViewById<TextView>(R.id.noSelectedProductLabel).isClickable = enable
        selectedProductLinkTextView.movementMethod = if (enable) LinkMovementMethod.getInstance() else null
        seekBar.isEnabled = enable
    }

    private fun flashView(view: View) {
        val animation = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f)
        animation.duration = 500 // Durata di ogni ciclo
        animation.repeatCount = 1
        animation.start()
    }

    private fun getStringValue(value: Double): String {
        return when (value) {
            Double.POSITIVE_INFINITY -> "∞"  // Se il numero è infinito, ritorna "∞"
            value.toInt().toDouble() -> value.toInt().toString()  // Se il numero è intero, rimuove i decimali
            else -> {
                val formattedValue = String.format(Locale.US, "%.2f", value)
                val formattedValueDouble = formattedValue.toDouble()
                if (formattedValueDouble == formattedValueDouble.toInt().toDouble()) {
                    formattedValueDouble.toInt().toString() // mostra senza decimali
                } else {
                    formattedValue // Mostra un solo decimale con punto
                }
            }
        }
    }

    private fun getDoubleValue(editText: EditText): Double {
        var value = editText.text.toString().toDoubleOrNull() ?: 0.0
        if (editText.text.toString().trim() == "∞") {
            value = Double.POSITIVE_INFINITY
        }
        return value;
    }

    private fun changeTextProgrammatically(editText: EditText, newText: String) {
        isProgrammaticChange = true
        editText.setText(newText)
        isProgrammaticChange = false
    }

    private fun updateSeekbar(editText: EditText) {
        if (editText === dilutionRatioEditText && selectedProductDilution != null) {
            val currentDilutionValue = getDoubleValue(editText)
            val progress = selectedProductDilution!!.value - currentDilutionValue.toInt()

            if (currentDilutionValue == Double.POSITIVE_INFINITY || progress < 0) {
                // TODO mostra errore fuori range minimo
                seekBar.progress = 0;
            } else if (progress > seekBar.max) {
                // TODO mostra errore fuori range massimo
                seekBar.progress = seekBar.max
            } else {
                seekBar.progress = progress
            }
        }
    }
}
