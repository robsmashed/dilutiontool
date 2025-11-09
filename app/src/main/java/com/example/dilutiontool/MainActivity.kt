package com.example.dilutiontool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale
import kotlin.math.roundToInt

data class Item(
    val id: ItemId,
    val label: String,
    val valueSuffix: String,
    val bgResId: Int = -1,
    var value: Double = 0.0,
)

enum class ItemId {
    QUANTITY,
    DILUTION,
    WATER,
    CONCENTRATE
}
class MainActivity : AppCompatActivity() {
    private var selectedProductWithDilutions: ProductWithDilutions? = null
    private var selectedProductDilution: Dilution? = null
    private lateinit var productContainer: LinearLayout
    private lateinit var selectedProductNameTextView: TextView
    private lateinit var selectedProductDescriptionTextView: TextView
    private lateinit var selectedProductImageView: ImageView
    private lateinit var selectedProductLinkTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var discardProductSelectionFab: FloatingActionButton
    private lateinit var selectedProductContainer: LinearLayout
    private lateinit var noSelectedProductLabel: TextView
    private lateinit var recycler: RecyclerView
    private val items = mutableListOf(
        Item(ItemId.QUANTITY, "Quantità totale", "ml"),
        Item(ItemId.DILUTION, "Rapporto di diluizione", ":1"),
        Item(ItemId.WATER, "Quantità di acqua", "ml"),
        Item(ItemId.CONCENTRATE, "Quantità di prodotto", "ml"),
    )
    private lateinit var draggableAdapter: DraggableAdapter

    class NoScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean {
            return false // Disabilita lo scroll verticale
        }
    }

    private val productSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDilution = result.data?.getParcelableExtra<Dilution>("selectedDilution")
            val selectedProductWithDilutions = result.data?.getParcelableExtra<ProductWithDilutions>("selectedProductWithDilutions")
            setSelectedProduct(selectedProductWithDilutions, selectedDilution)
        }
    }

    private fun setSelectedProduct(selectedProductWithDilutions: ProductWithDilutions?, selectedDilution: Dilution?) {
        if (selectedProductWithDilutions != null && selectedDilution != null) {
            val item = items.find { it.id == ItemId.DILUTION }
            var currentDilutionValue = item!!.value // current dilution value in input text
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
            selectedProductNameTextView.text = selectedProductWithDilutions.product.name

            selectedProductDescriptionTextView.text = getDescription(selectedDilution)
            selectedProductDescriptionTextView.setOnClickListener {
                selectedProductWithDilutions.let { product ->
                    ProductDialogUtils.showDilutionDialog(this, product) { dilution ->
                        setSelectedProduct(product, dilution)
                    }
                }
            }

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
                            draggableAdapter.calculateResult(ItemId.DILUTION, (selectedDilution.value - progress).toDouble())
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
            draggableAdapter.calculateResult(ItemId.DILUTION, currentDilutionValue)
        } else {
            discardCurrentProductSelection()
        }
    }

    private fun discardCurrentProductSelection() {
        selectedProductDilution = null
        //updateDilutionRangeWarning() TODO
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

    /* Disabling main menu with settings option
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
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        productContainer = findViewById(R.id.productContainer)
        selectedProductNameTextView = findViewById(R.id.selectedProductName)
        selectedProductDescriptionTextView = findViewById(R.id.selectedProductDescription)
        selectedProductImageView = findViewById(R.id.selectedProductImage)
        selectedProductLinkTextView = findViewById(R.id.selectedProductLinkTextView)
        seekBar = findViewById(R.id.seekBar)
        selectedProductContainer = findViewById(R.id.selectedProductContainer)
        val selectedProduct = findViewById<LinearLayout>(R.id.selectedProduct)
        noSelectedProductLabel = findViewById(R.id.noSelectedProductLabel)
        discardProductSelectionFab = findViewById(R.id.discardProductSelectionFab)

        recycler = findViewById(R.id.recyclerTop)
        draggableAdapter = DraggableAdapter(items) // Inizializza l'adapter
        val itemTouchHelper = ItemTouchHelper(DragManageAdapter(draggableAdapter, items) { enable ->
            productContainer.alpha = if (enable) 1.0f else 0.5f // Imposta l'alpha per dare l'effetto di disabilitazione
            selectedProductContainer.isClickable = enable
            noSelectedProductLabel.isClickable = enable
            selectedProductLinkTextView.movementMethod = if (enable) LinkMovementMethod.getInstance() else null
            seekBar.isEnabled = enable
        }) // Inizializza l'ItemTouchHelper
        draggableAdapter.touchHelper = itemTouchHelper // Associa l'ItemTouchHelper all'adapter
        draggableAdapter.onDilutionRatioChange = { currentDilutionValue ->
            // update seekbar
            if (selectedProductDilution != null) {
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
        recycler.apply { // Imposta l'adapter al RecyclerView e il layout manager
            layoutManager = NoScrollLinearLayoutManager(this@MainActivity)
            adapter = draggableAdapter
        }
        itemTouchHelper.attachToRecyclerView(recycler) // Attacca l'ItemTouchHelper al RecyclerView

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        discardProductSelectionFab.setOnClickListener {
            discardCurrentProductSelection()
        }

        val launchProductList = View.OnClickListener {
            launchProductListActivity()
        }
        selectedProduct.setOnClickListener(launchProductList)
        noSelectedProductLabel.setOnClickListener(launchProductList)
    }

    /* TODO
    private fun updateDilutionRangeWarning() {
        if (selectedProductDilution != null && (selectedProductDilution!!.minValue > getDoubleValue(dilutionRatioEditText) || selectedProductDilution!!.value < getDoubleValue(dilutionRatioEditText))) {
            val warningIcon = ContextCompat.getDrawable(this, R.drawable.baseline_warning_24)
            warningIcon?.setBounds(0, 0, warningIcon.intrinsicWidth, warningIcon.intrinsicHeight)
            dilutionRatioEditText.setError("Diluizione fuori range prodotto", warningIcon)
        } else {
            dilutionRatioEditText.error = null
        }
    }
    */

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
}
