package com.example.dilutiontool

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dilutiontool.data.database.AppDatabase
import com.example.dilutiontool.data.database.AppDatabase.Companion.getDatabase
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.Product
import com.example.dilutiontool.entity.ProductWithDilutions
import java.util.concurrent.Executors

class ProductAddActivity : AppCompatActivity() {
    private var dilutionListLayouts: MutableMap<String, MutableList<LinearLayout>> = mutableMapOf()

    private lateinit var productNameInput: EditText
    private lateinit var productDescriptionInput: EditText
    private lateinit var productImageUrlInput: EditText
    private lateinit var productLinkInput: EditText
    private lateinit var dilutionGroups: LinearLayout
    private lateinit var addDilutionGroupButton: Button
    private lateinit var saveProductButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        productNameInput = findViewById(R.id.productNameInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productImageUrlInput = findViewById(R.id.productImageUrlInput)
        productLinkInput = findViewById(R.id.productLinkInput)
        dilutionGroups = findViewById(R.id.dilutionGroups)
        addDilutionGroupButton = findViewById(R.id.addDilutionGroupButton)
        saveProductButton = findViewById(R.id.saveProductButton)

        val productWithDilutions = intent.getParcelableExtra<ProductWithDilutions>("PRODUCT_WITH_DILUTIONS")
        if (productWithDilutions != null) {
            productWithDilutions.let {
                productNameInput.setText(it.product.name)
                productDescriptionInput.setText(it.product.description)
                productImageUrlInput.setText(it.product.imageUrl)
                productLinkInput.setText(it.product.link)
            }

            val groupedByMode: Map<String, List<Dilution>> = productWithDilutions.dilutions.groupBy { it.mode ?: "" }
            groupedByMode.forEach { (mode, dilutionList) ->
                addDilutionGroup(mode, dilutionList)
            }
        } else {
            addDilutionGroup()
        }

        addDilutionGroupButton.setOnClickListener {
            addDilutionGroup()
        }

        saveProductButton.setOnClickListener {
            val product = Product(
                id = productWithDilutions?.product?.id ?: 0,
                name = productNameInput.text.toString(),
                description = productDescriptionInput.text.toString(),
                imageUrl = productImageUrlInput.text.toString(),
                link = productLinkInput.text.toString()
            )

            val dilutions = mutableListOf<Dilution>()

            dilutionListLayouts.forEach { (groupName, layoutList) ->
                for (dilutionListLayout in layoutList) {
                    for (i in 0 until dilutionListLayout.childCount) {
                        val dilutionRow = dilutionListLayout.getChildAt(i)
                        var dilutionValue = dilutionRow.findViewById<EditText>(R.id.dilutionInput).text.toString().toIntOrNull()
                        var minDilutionValue = dilutionRow.findViewById<EditText>(R.id.minDilutionInput).text.toString().toIntOrNull()
                        if (dilutionValue != null || minDilutionValue != null) {
                            if (dilutionValue != null && minDilutionValue != null && minDilutionValue > dilutionValue) {
                                dilutionValue = minDilutionValue.also { minDilutionValue = dilutionValue }
                            }
                            val dilution = Dilution(
                                productId = 0, // Lo inseriamo poi in fase di saveProductWithDilutions
                                description = dilutionRow.findViewById<EditText>(R.id.dilutionDescriptionInput).text.toString(),
                                value = dilutionValue ?: minDilutionValue ?: 0,
                                minValue = minDilutionValue ?: dilutionValue ?: 0,
                                mode = groupName.ifEmpty { null },
                            )
                            dilutions.add(dilution)
                        }
                    }
                }
            }

            if (product.name.isEmpty() && dilutions.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome e almeno una diluizione valida", Toast.LENGTH_SHORT).show()
            } else if (product.name.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome valido", Toast.LENGTH_SHORT).show()
            } else if (dilutions.isEmpty()) {
                Toast.makeText(this, "Inserisci almeno una diluizione valida", Toast.LENGTH_SHORT).show()
            } else {
                saveProductWithDilutions(ProductWithDilutions(
                    product = product,
                    dilutions = dilutions
                ))
            }
        }

    }

    private fun saveProductWithDilutions(productWithDilutions: ProductWithDilutions) {
        if (productWithDilutions.product.name != "" && productWithDilutions.dilutions.isNotEmpty()) {
            val db: AppDatabase = getDatabase(this)

            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                val product = productWithDilutions.product
                val productId = db.productDao().insertProduct(product) // Inserisci il prodotto nel database (Room genererà automaticamente l'ID)

                // Ora inserisci le diluizioni, associandole all'ID del prodotto
                val dilutions = productWithDilutions.dilutions.map {
                    it.copy(productId = productId)  // Usa l'ID del prodotto
                }
                db.productDao().deleteDilutionsForProduct(productId) // elimina quelle già esistenti, se ci sono
                db.productDao().insertDilutions(dilutions) // Inserisci le nuove diluizioni nel database

                runOnUiThread {
                    val resultIntent = Intent()
                    setResult(RESULT_OK, resultIntent)  // Imposta il risultato
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "Inserisci un nome e una diluizione valida, es. 5:1", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addDilutionGroup(groupName: String = "", dilutions: List<Dilution> = emptyList()) {
        val newDilutionsGroup = LayoutInflater.from(this).inflate(R.layout.dilution_group_layout, dilutionGroups, false)
        newDilutionsGroup.findViewById<EditText>(R.id.dilutionGroupNameEditText).setText(groupName)

        newDilutionsGroup.findViewById<Button>(R.id.removeDilutionGroupButton).setOnClickListener {
            dilutionGroups.removeView(newDilutionsGroup)
            dilutionListLayouts.remove(groupName)
        }

        val dilutionListLayout = newDilutionsGroup.findViewById<LinearLayout>(R.id.dilutionListLayout)

        newDilutionsGroup.findViewById<Button>(R.id.addDilutionButton).setOnClickListener {
            addDilutionRow(dilutionListLayout)
        }

        if (dilutions.isEmpty()) {
            addDilutionRow(dilutionListLayout)
        } else {
            val sortedDilutions = dilutions.sortedBy { it.minValue }
            for (dilution in sortedDilutions) {
                addDilutionRow(dilutionListLayout, dilution)
            }
        }

        if (dilutionListLayouts[groupName] === null) {
            dilutionListLayouts[groupName] = mutableListOf()
        }
        dilutionListLayouts[groupName]?.add(dilutionListLayout)

        dilutionGroups.addView(newDilutionsGroup)
    }

    private fun addDilutionRow(dilutionListLayout: LinearLayout, dilution: Dilution? = null) {
        val newDilutionRow = LayoutInflater.from(this).inflate(R.layout.dilution_row_layout, dilutionListLayout, false)

        newDilutionRow.findViewById<Button>(R.id.removeDilutionButton).setOnClickListener {
            dilutionListLayout.removeView(newDilutionRow)
        }

        if (dilution != null) {
            newDilutionRow.findViewById<EditText>(R.id.minDilutionInput).setText(dilution.minValue.toString())
            newDilutionRow.findViewById<EditText>(R.id.dilutionInput).setText(dilution.value.toString())
            newDilutionRow.findViewById<EditText>(R.id.dilutionDescriptionInput).setText(dilution.description)
        }

        dilutionListLayout.addView(newDilutionRow)
    }
}
