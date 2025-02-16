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

    private lateinit var productNameInput: EditText
    private lateinit var productDescriptionInput: EditText
    private lateinit var productImageUrlInput: EditText
    private lateinit var productLinkInput: EditText
    private lateinit var dilutionListLayout: LinearLayout
    private lateinit var addDilutionButton: Button
    private lateinit var saveProductButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        productNameInput = findViewById(R.id.productNameInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productImageUrlInput = findViewById(R.id.productImageUrlInput)
        productLinkInput = findViewById(R.id.productLinkInput)
        dilutionListLayout = findViewById(R.id.dilutionListLayout)
        addDilutionButton = findViewById(R.id.addDilutionButton)
        saveProductButton = findViewById(R.id.saveProductButton)

        val productWithDilutions = intent.getParcelableExtra<ProductWithDilutions>("PRODUCT_WITH_DILUTIONS")
        if (productWithDilutions != null) {
            productWithDilutions.let {
                productNameInput.setText(it.product.name)
                productDescriptionInput.setText(it.product.description)
                productImageUrlInput.setText(it.product.imageUrl)
                productLinkInput.setText(it.product.link)
            }
            productWithDilutions.dilutions.forEach { dilution -> addDilutionRow(dilution)}
        } else {
            addDilutionRow();
        }

        addDilutionButton.setOnClickListener {
            addDilutionRow();
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
                    )
                    dilutions.add(dilution)
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

    private fun addDilutionRow(dilution: Dilution? = null) {
        val newDilutionRow = LayoutInflater.from(this)
            .inflate(R.layout.dilution_row_layout, dilutionListLayout, false)

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
