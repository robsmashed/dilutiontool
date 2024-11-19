package com.example.dilutiontool

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var dilutionDescriptionInput: EditText
    private lateinit var dilutionInput: EditText
    private lateinit var dilutionListLayout: LinearLayout
    private lateinit var addDilutionButton: Button
    private lateinit var saveProductButton: Button
    private val dilutionList = mutableListOf<Dilution>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        productNameInput = findViewById(R.id.productNameInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productImageUrlInput = findViewById(R.id.productImageUrlInput)
        productLinkInput = findViewById(R.id.productLinkInput)
        dilutionDescriptionInput = findViewById(R.id.dilutionDescriptionInput)
        dilutionInput = findViewById(R.id.dilutionInput)
        dilutionListLayout = findViewById(R.id.dilutionListLayout)
        addDilutionButton = findViewById(R.id.addDilutionButton)
        saveProductButton = findViewById(R.id.saveProductButton)

        addDilutionButton.setOnClickListener {
            val descriptionText = dilutionDescriptionInput.text.toString()
            val dilutionValue = dilutionInput.text.toString().toIntOrNull()

            if (descriptionText.isNotEmpty() && dilutionValue != null) {
                val dilution = Dilution(
                    productId = 0,
                    description = descriptionText,
                    value = dilutionValue
                )
                dilutionList.add(dilution)
                addDilutionToLayout(descriptionText, dilutionValue)
                dilutionDescriptionInput.text.clear()
                dilutionInput.text.clear()
            } else {
                Toast.makeText(this, "Inserisci una descrizione e una diluizione valida, es. 5:1", Toast.LENGTH_SHORT).show()
            }
        }

        saveProductButton.setOnClickListener {
            val product = Product(
                id = 0,
                name = productNameInput.text.toString(),
                description = productDescriptionInput.text.toString(),
                imageUrl = productImageUrlInput.text.toString(),
                link = productLinkInput.text.toString()
            )
            val productWithDilutions = ProductWithDilutions(
                product = product,
                dilutions = dilutionList
            )
            saveProductWithDilutions(productWithDilutions)
        }
    }

    private fun addDilutionToLayout(description: String, value: Int) {
        val textView = TextView(this).apply {
            text = "$description: 1:$value"
            textSize = 16f
            setPadding(8, 8, 8, 8)
        }
        dilutionListLayout.addView(textView)
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
                    it.copy(productId = productId)  // Usa l'ID del prodotto generato
                }
                db.productDao().insertDilutions(dilutions) // Inserisci le diluizioni nel database

                runOnUiThread {
                    Toast.makeText(this, "Prodotto aggiunto!", Toast.LENGTH_SHORT).show() // Mostra un messaggio di conferma
                    val resultIntent = Intent()
                    setResult(RESULT_OK, resultIntent)  // Imposta il risultato
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "Inserisci un nome e una diluizione valida, es. 5:1", Toast.LENGTH_SHORT).show()
        }
    }
}
