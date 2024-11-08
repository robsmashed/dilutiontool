package com.example.dilutiontool

import android.content.Intent
import android.os.Bundle
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dilutiontool.data.database.AppDatabase
import com.example.dilutiontool.data.database.AppDatabase.Companion.getDatabase
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions
import java.util.concurrent.Executors

class ProductListActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        db = getDatabase(this)

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val products = db.productDao().getAllProductsWithDilutions()

            runOnUiThread {
                val productRecyclerView: RecyclerView = findViewById(R.id.productRecyclerView)
                productRecyclerView.layoutManager = LinearLayoutManager(this@ProductListActivity)
                productRecyclerView.adapter = ProductAdapter(this@ProductListActivity, products) { selectedProduct ->
                    // TODO use only one function
                    if (selectedProduct.dilutions.all { it.mode == null })
                        showDilutionSelectionDialog(selectedProduct)
                    else
                        showDialogWithCategorizedItems(selectedProduct)
                }
            }
        }
    }

    private fun getDescription(dilution: Dilution): String {
        var dilutionDescription = "puro"
        if (dilution.value > 0) {
            if (dilution.minValue != null) {
                dilutionDescription = "1:${dilution.minValue} - 1:${dilution.value}"
            } else {
                dilutionDescription = "1:${dilution.value}"
            }
        }
        return "${dilution.description} ($dilutionDescription)"
    }

    private fun showDilutionSelectionDialog(productWithDilutions: ProductWithDilutions) {
        AlertDialog.Builder(this)
            .setTitle("Diluizione per ${productWithDilutions.product.name}")
            .setNegativeButton("Annulla", null)
            .setItems(productWithDilutions.dilutions.map { getDescription(it) }.toTypedArray()) { _, which ->
                val selectedDilution = productWithDilutions.dilutions[which]
                val resultIntent = Intent().apply {
                    putExtra("selectedDilution", selectedDilution.value)
                    putExtra("productName", productWithDilutions.product.name)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .show()
    }

    fun showDialogWithCategorizedItems(productWithDilutions: ProductWithDilutions) {
        // Definisci le categorie e gli elementi per ciascuna categoria
        val categories = productWithDilutions.dilutions.mapNotNull { it.mode }.distinct()

        val items = productWithDilutions.dilutions.groupBy { it.mode }
            .values
            .toList()

        // Crea una lista di mappe per la struttura dei dati
        val groupData = categories.map { mapOf("CATEGORY" to it) }
        val childData = items.map { it.map { item -> mapOf("ITEM" to getDescription(item)) } }

        // Crea un SimpleExpandableListAdapter
        val expandableListAdapter = SimpleExpandableListAdapter(
            this,
            groupData,
            android.R.layout.simple_expandable_list_item_1,
            arrayOf("CATEGORY"),
            intArrayOf(android.R.id.text1),
            childData,
            android.R.layout.simple_list_item_1,
            arrayOf("ITEM"),
            intArrayOf(android.R.id.text1)
        )

        // Crea l'ExpandableListView
        val expandableListView = ExpandableListView(this)
        expandableListView.setAdapter(expandableListAdapter)

        // Imposta il listener per il click sui bambini (elementi)
        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val selectedDilution = items[groupPosition][childPosition]
            val resultIntent = Intent().apply {
                putExtra("selectedDilution", selectedDilution.value)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
            true
        }

        // Crea l'AlertDialog
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Diluizione per ${productWithDilutions.product.name}")
            .setView(expandableListView)
            .setNegativeButton("Annulla", null)
            .create()

        alertDialog.show()
    }
}
