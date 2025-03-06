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
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private var selectedProductWithDilutions: ProductWithDilutions? = null
    private var selectedProductDilution: Dilution? = null
    private lateinit var dilutionRatioEditText: EditText
    private lateinit var totalLiquidEditText: EditText
    private lateinit var concentrateEditText: EditText
    private lateinit var waterEditText: EditText
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
            setSelectedProduct(selectedProductWithDilutions, selectedDilution)

            dilutionRatioEditText.clearFocus()
            totalLiquidEditText.clearFocus()
            concentrateEditText.clearFocus()
            waterEditText.clearFocus()
        }
    }

    private fun setSelectedProduct(selectedProductWithDilutions: ProductWithDilutions?, selectedDilution: Dilution?) {
        if (selectedProductWithDilutions != null && selectedDilution != null) {
            var currentDilutionValue = getDoubleValue(dilutionRatioEditText) // current dilution value in input text
            if ( // check if same product & current dilution is in dilution range
                selectedProductWithDilutions.product.id != this.selectedProductWithDilutions?.product?.id ||
                (currentDilutionValue < selectedDilution.minValue || currentDilutionValue > selectedDilution.value)
            ) {
                currentDilutionValue = selectedDilution.value.toDouble() // reset to the lightest dilution of the product
            }

            // Initialize view
            discardProductSelectionFab.visibility = View.VISIBLE
            selectedProductDilution = selectedDilution
            this.selectedProductWithDilutions = selectedProductWithDilutions
            dilutionRatioEditText.setText(getStringValue(currentDilutionValue))
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
            selectedProductLinkTextView.visibility = if (selectedProductWithDilutions.product.link.isNullOrBlank()) View.INVISIBLE else View.VISIBLE

            Glide.with(this)
                .load(selectedProductWithDilutions.product.image)
                .placeholder(R.drawable.product_loading)
                .error(R.drawable.product_loading)
                .into(selectedProductImageView)

            // Initialize seekbar
            if (selectedDilution.minValue != selectedDilution.value) {
                seekBar.progress = 0
                seekBar.max = selectedDilution.value - selectedDilution.minValue
                seekBar.visibility = View.VISIBLE
                seekBar.progress = selectedDilution.value - currentDilutionValue.roundToInt()
                seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            dilutionRatioEditText.setText(getStringValue((selectedDilution.value - progress).toDouble()))
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            } else {
                seekBar.visibility = View.GONE
            }

            selectedProductContainer.visibility = View.VISIBLE
            noSelectedProductLabel.visibility = View.GONE
            updateDilutionRangeWarning()
        } else {
            discardCurrentProductSelection()
        }
    }

    private fun discardCurrentProductSelection() {
        selectedProductDilution = null
        updateDilutionRangeWarning()
        discardProductSelectionFab.visibility = View.GONE
        selectedProductContainer.visibility = View.GONE
        noSelectedProductLabel.visibility = View.VISIBLE
        seekBar.visibility = View.GONE
    }

    private fun launchProductListActivity() {
        val intent = Intent(this, ProductListActivity::class.java)
        intent.putExtra("selectedDilution", selectedProductDilution)
        intent.putExtra("selectedProductWithDilutions", selectedProductWithDilutions)
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

    private fun limitDecimalInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val text = editable.toString()
                if (text.contains(".")) {
                    val parts = text.split(".")
                    if (parts.size == 2 && parts[1].length > 2) {
                        val newText = parts[0] + "." + parts[1].substring(0, 2)
                        editText.setText(newText)
                        editText.setSelection(newText.length)
                    }
                }
            }
        })
    }

    private fun calculateResult(currentEditText: EditText) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        dilutionRatioEditText = findViewById(R.id.dilutionRatioEditText)
        totalLiquidEditText = findViewById(R.id.totalEditText)
        concentrateEditText = findViewById(R.id.concentrateEditText)
        waterEditText = findViewById(R.id.waterEditText)
        productContainer = findViewById(R.id.productContainer)
        selectedProductNameTextView = findViewById(R.id.selectedProductName)
        selectedProductDescriptionTextView = findViewById(R.id.selectedProductDescription)
        selectedProductImageView = findViewById(R.id.selectedProductImage)
        selectedProductLinkTextView = findViewById(R.id.selectedProductLinkTextView)
        seekBar = findViewById(R.id.seekBar)
        selectedProductContainer = findViewById(R.id.selectedProductContainer)
        noSelectedProductLabel = findViewById(R.id.noSelectedProductLabel)
        discardProductSelectionFab = findViewById(R.id.discardProductSelectionFab)
        val totalLiquidLockCheckBox = findViewById<CheckBox>(R.id.totalLiquidLockCheckBox)
        val dilutionRatioLockCheckBox = findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox)
        val waterResultTextLockCheckBox = findViewById<CheckBox>(R.id.waterResultTextLockCheckBox)
        val resultTextLockCheckBox = findViewById<CheckBox>(R.id.resultTextLockCheckBox)

        discardProductSelectionFab.setOnClickListener {
            discardCurrentProductSelection()
        }

        limitDecimalInput(dilutionRatioEditText)
        limitDecimalInput(totalLiquidEditText)
        limitDecimalInput(concentrateEditText)
        limitDecimalInput(waterEditText)

        val checkBoxEditTextMap = mapOf(
            totalLiquidLockCheckBox to totalLiquidEditText,
            dilutionRatioLockCheckBox to dilutionRatioEditText,
            waterResultTextLockCheckBox to waterEditText,
            resultTextLockCheckBox to concentrateEditText
        )
        checkBoxEditTextMap.keys.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBoxEditTextMap.keys.count { it.isChecked } >= 2) {
                    for ((otherCheckBox, otherEditText) in checkBoxEditTextMap) {
                        if (otherCheckBox.isChecked) {
                            otherEditText.isEnabled = isChecked // Imposta isEnabled dell'EditText in base allo stato della checkBox
                        } else {
                            otherCheckBox.isEnabled = false // Disabilita le altre CheckBox non selezionate
                        }
                    }
                    enableProductSelection(findViewById<CheckBox>(R.id.dilutionRatioLockCheckBox).isChecked)
                } else {
                    for ((otherCheckBox, otherEditText) in checkBoxEditTextMap) {
                        otherEditText.isEnabled = false // Disabilita tutti i campi di testo
                        otherCheckBox.isEnabled = true // Riabilita tutte le checkbox
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

        setTextInputBehaviour(dilutionRatioEditText)
        setTextInputBehaviour(waterEditText)
        setTextInputBehaviour(concentrateEditText)
        setTextInputBehaviour(totalLiquidEditText)

        calculateResult(totalLiquidEditText) // inizializzazione a partire dai due valori iniziali
    }

    private fun setTextInputBehaviour(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSeekbar(editText)
                if (!isProgrammaticChange) {
                    calculateResult(editText)
                }
                updateDilutionRangeWarning()
            }
        })
        editText.setSelectAllOnFocus(true);
        editText.setOnClickListener {
            editText.clearFocus()
            editText.requestFocus()
        }
    }

    private fun updateDilutionRangeWarning() {
        if (selectedProductDilution != null && (selectedProductDilution!!.minValue > getDoubleValue(dilutionRatioEditText) || selectedProductDilution!!.value < getDoubleValue(dilutionRatioEditText))) {
            dilutionRatioEditText.error = "Diluizione fuori range selezionato"
        } else {
            dilutionRatioEditText.error = null
        }
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
                seekBar.progress = 0
            } else if (progress > seekBar.max) {
                seekBar.progress = seekBar.max
            } else {
                seekBar.progress = progress
            }
        }
    }
}
