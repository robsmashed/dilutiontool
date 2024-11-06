package com.example.dilutiontool // Assicurati che il pacchetto sia corretto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        // Supponiamo che tu abbia una lista di prodotti da mostrare
        val baseUrl = "https://www.lacuradellauto.it/web/image/product.product/"
        val products = listOf(
            Product(
                "LCDASC-500",
                "LCDA SuperClean",
                "Un detergente talmente potente da poter essere utilizzato puro per la pulizia di cerchi e motore e, allo stesso tempo, estremamente delicato da poter essere usato sulla pelle più delicata. Questo è SuperClean!",
                arrayOf(
                    Dilution("Sporco grave (da 1:1 a 5:1)", 5),
                    Dilution("Sporco medio (1:10)", 10),
                    Dilution("Sporco leggero e pulizia di mantenimento (1:20)", 20)
                ),
                baseUrl + "3658/image_1920/lcdasc-lcda-superclean"
            ),
            Product(
                "LAB02",
                "Labocosmetica Semper Shampoo Neutro",
                "Semper è uno shampoo neutro di mantenimento, super concentrato e fortemente lubrificato.",
                arrayOf(
                    Dilution("Sporco ostinato (1:800)", 800),
                    Dilution("Sporco medio (1:1000)", 1000),
                    Dilution("Sporco leggero (1:1200)", 1200)
                ),
                "3701/image_1024/mixlabsem-labocosmetica-semper-shampoo-neutro"
            )
        )

        val productRecyclerView: RecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = LinearLayoutManager(this)
        productRecyclerView.adapter = ProductAdapter(products) { selectedProduct ->
            // Quando un prodotto viene selezionato, mostriamo il dialog per scegliere la diluizione
            showDilutionSelectionDialog(selectedProduct)
        }
    }

    private fun showDilutionSelectionDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Seleziona una diluizione per ${product.name}")
            .setItems(product.dilutions.map { it.description }.toTypedArray()) { dialog, which ->
                val selectedDilution = product.dilutions[which]  // Ottieni l'oggetto Dilution corrispondente
                val resultIntent = Intent().apply {
                    putExtra("selectedDilution", selectedDilution.value)
                    putExtra("productName", product.name)
                }
                setResult(RESULT_OK, resultIntent)
                finish() // Chiudiamo la ProductListActivity
            }
            .show()
    }
}
