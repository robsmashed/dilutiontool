package com.example.dilutiontool

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ExpandableListView
import android.widget.SearchView
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.data.database.AppDatabase
import com.example.dilutiontool.data.database.AppDatabase.Companion.getDatabase
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.Executors

class ProductListActivity : AppCompatActivity() {
    private val PICK_FILE_REQUEST_CODE = 1
    private val EXPORT_FILE_REQUEST_CODE = 1001
    private lateinit var db: AppDatabase
    private lateinit var products: List<ProductWithDilutions>
    private lateinit var filteredProducts: List<ProductWithDilutions>
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var fabDeleteProduct: FloatingActionButton
    private lateinit var noProductsText: TextView
    private var selectedProducts: List<ProductWithDilutions> = emptyList()

    private var selectedProductWithDilutions: ProductWithDilutions? = null
    private var selectedDilution: Dilution? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EXPORT_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val resolver: ContentResolver = contentResolver
                    val outputStream: OutputStream? = resolver.openOutputStream(uri)

                    val gson = Gson()
                    val json = gson.toJson(products)

                    outputStream?.write(json.toByteArray())
                    outputStream?.close()

                    Toast.makeText(this, "File salvato con successo a: $uri", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Errore durante il salvataggio del file.", Toast.LENGTH_SHORT).show()
                }
            }
            updateSelectedProducts(emptyList())
            fetchProducts()
        } else if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = InputStreamReader(inputStream)
                    val gson = Gson()

                    // Leggi il JSON dal file e deserializzalo in una lista di Oggetti
                    val productsWithDilutions: List<ProductWithDilutions> = gson.fromJson(reader, Array<ProductWithDilutions>::class.java).toList()
                    reader.close()

                    val executor = Executors.newSingleThreadExecutor()
                    executor.execute {
                        productsWithDilutions.forEach { productWithDilution ->
                            val product = productWithDilution.product
                            val productId = db.productDao().insertProduct(product.copy(id = 0)) // Inserisci il prodotto nel database (Room genererà automaticamente l'ID)

                            // Ora inserisci le diluizioni, associandole all'ID del prodotto
                            val dilutions = productWithDilution.dilutions.map {
                                it.copy(id = 0, productId = productId)  // Usa l'ID del prodotto
                            }
                            // db.productDao().deleteDilutionsForProduct(productId) // elimina quelle già esistenti, se ci sono
                            db.productDao().insertDilutions(dilutions) // Inserisci le nuove diluizioni nel database
                        }

                        runOnUiThread {
                            updateSelectedProducts(emptyList())
                            fetchProducts()
                            Toast.makeText(this, productsWithDilutions.size.toString() + " nuovi prodotti inseriti", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, " Errore nell'aggiunta dei prodotti selezionati", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        updateSelectedProducts(emptyList())
        fetchProducts()

        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Prodotto " + if (result.data?.getBooleanExtra("isAdd", true) === true) "aggiunto" else "modificato" + " correttamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_product_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/json"
                startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
                true
            }
            R.id.action_export -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "dilute-export.json")
                }
                startActivityForResult(intent, EXPORT_FILE_REQUEST_CODE)
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onDeleteProductsClick(productsToDelete: List<ProductWithDilutions>) {
        val productNames = productsToDelete.joinToString("\n") { "• ${it.product.name}" }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare i prodotti selezionati?\n\n$productNames")
            .setPositiveButton("Conferma") { _, _ ->
                deleteProducts(productsToDelete.toList())
            }
            .setNegativeButton("Annulla", null)
            .create()
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        selectedDilution = intent.getParcelableExtra("selectedDilution")
        selectedProductWithDilutions = intent.getParcelableExtra("selectedProductWithDilutions")

        productRecyclerView = findViewById(R.id.productRecyclerView)

        fabAddProduct = findViewById(R.id.addFab)
        fabDeleteProduct = findViewById(R.id.deleteFab)
        fabAddProduct.setOnClickListener {
            addProduct()
        }
        fabDeleteProduct.setOnClickListener {
            onDeleteProductsClick(selectedProducts)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Aggiungi l'icona di ritorno (freccia) alla Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_24)


        noProductsText = findViewById(R.id.noProductsText)
        noProductsText.setOnClickListener {
            addProduct()
        }

        searchView = findViewById(R.id.searchView)
        searchView.setIconifiedByDefault(false) // Espandi la SearchView di default

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Chiudi la tastiera
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocusView = currentFocus
                if (currentFocusView != null) {
                    inputMethodManager.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filteredProducts = products.filter { productWithDilutions ->
                        val product = productWithDilutions.product
                        product.name.contains(
                            newText,
                            ignoreCase = true
                        ) || product.description?.contains(newText, ignoreCase = true) ?: false
                    }
                    (productRecyclerView.adapter as ProductAdapter).updateList(filteredProducts)
                }
                return true
            }
        })

        db = getDatabase(this)
        fetchProducts()
    }

    private fun addProduct(productWithDilutions: ProductWithDilutions? = null) {
        val intent = Intent(this, ProductAddActivity::class.java)
        if (productWithDilutions != null) {
            intent.putExtra("PRODUCT_WITH_DILUTIONS", productWithDilutions)
        }
        activityResultLauncher.launch(intent)
    }

    private fun fetchProducts() {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            // Recupera i prodotti
            products = db.productDao().getAllProductsWithDilutionsSortedByNameAsc()
            filteredProducts = products

            runOnUiThread {
                // Gestisci la visibilità degli elementi
                noProductsText.visibility = if (products.isNotEmpty()) View.GONE else View.VISIBLE
                searchView.visibility = if (products.isNotEmpty()) View.VISIBLE else View.GONE
                productRecyclerView.visibility = if (products.isNotEmpty()) View.VISIBLE else View.GONE

                // Imposta il layoutManager e l'adapter
                productRecyclerView.layoutManager = LinearLayoutManager(this@ProductListActivity)
                productRecyclerView.adapter = ProductAdapter(
                    this@ProductListActivity,
                    filteredProducts,
                    { selectedProduct ->
                        // TODO use only one dynamic dialog
                        if (selectedProduct.dilutions.all { it.mode == null })
                            showDilutionSelectionDialog(selectedProduct)
                        else
                            showDialogWithCategorizedItems(selectedProduct)
                    },
                    { selectedProducts -> updateSelectedProducts(selectedProducts.toList()) },
                    { productToDelete -> onDeleteProductsClick(listOf(productToDelete)) },
                    { productToEdit -> addProduct(productToEdit) },
                )
            }
        }
    }

    private fun updateSelectedProducts(selectedProducts: List<ProductWithDilutions>) {
        this.selectedProducts = selectedProducts
        fabAddProduct.visibility = if (this.selectedProducts.isNotEmpty()) View.GONE else View.VISIBLE
        fabDeleteProduct.visibility = if (this.selectedProducts.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteProducts(productsWithDilutions: List<ProductWithDilutions>) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val deletedIds = db.productDao().deleteProductsAndDilutions(productsWithDilutions)

            runOnUiThread {
                updateSelectedProducts(emptyList())
                fetchProducts()

                if (deletedIds.size > 1) {
                    Toast.makeText(this, "I prodotti selezionati sono stati rimossi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Il prodotto è stato rimosso", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setSelectedProductWithDilution(
        selectedDilution: Dilution,
        selectedProductWithDilutions: ProductWithDilutions
    ) {
        setResultIntent(selectedDilution, selectedProductWithDilutions)
        finish()
    }

    override fun onBackPressed() {
        if (selectedProducts.isNotEmpty()) {
            updateSelectedProducts(emptyList())
            fetchProducts()
        } else {
            val foundProduct: ProductWithDilutions? = products.find { it.product.id == selectedProductWithDilutions?.product?.id }
            val foundDilution = foundProduct?.dilutions?.find { it.id == selectedDilution?.id }
            setResultIntent(foundDilution, foundProduct)
            super.onBackPressed()
        }
    }

    private fun setResultIntent(
        selectedDilution: Dilution?,
        selectedProductWithDilutions: ProductWithDilutions?
    ) {
        var dilution = selectedDilution;
        var product = selectedProductWithDilutions
        if (selectedDilution == null || selectedProductWithDilutions == null) { // can't pass product without dilution and viceversa
            dilution = null
            product = null
        }
        val resultIntent = Intent().apply {
            putExtra("selectedDilution", dilution)
            putExtra("selectedProductWithDilutions", product)
        }
        setResult(RESULT_OK, resultIntent)
    }

    private fun showDilutionSelectionDialog(productWithDilutions: ProductWithDilutions) {
        val sortedDilutions = productWithDilutions.dilutions.sortedBy { it.minValue }
        val context = this

        val adapter = ArrayAdapter(
            context,
            R.layout.dialog_list_item,
            sortedDilutions.map { getDescription(it) }
        )

        AlertDialog.Builder(context)
            .setTitle("Diluizioni per ${productWithDilutions.product.name}")
            .setNegativeButton("Annulla", null)
            .setAdapter(adapter) { _, which ->
                setSelectedProductWithDilution(
                    sortedDilutions[which],
                    productWithDilutions
                )
            }
            .show()
    }


    fun showDialogWithCategorizedItems(productWithDilutions: ProductWithDilutions) {
        val sortedDilutions = productWithDilutions.dilutions.sortedBy { it.minValue }
        val categories = sortedDilutions.map { it.mode }.distinct()
        val items = sortedDilutions.groupBy { it.mode }.values.toList()

        // Crea una lista di mappe per la struttura dei dati
        val groupData = categories.map { mapOf("CATEGORY" to (it ?: "Categoria non definita")) }
        val childData = items.map { it.map { item -> mapOf("ITEM" to getDescription(item)) } }

        // Crea un SimpleExpandableListAdapter
        val expandableListAdapter = SimpleExpandableListAdapter(
            this,
            groupData,
            android.R.layout.simple_expandable_list_item_1,
            arrayOf("CATEGORY"),
            intArrayOf(android.R.id.text1),
            childData,
            R.layout.dialog_list_item,
            arrayOf("ITEM"),
            intArrayOf(android.R.id.text1)
        )

        // Crea l'ExpandableListView
        val expandableListView = ExpandableListView(this)
        expandableListView.setAdapter(expandableListAdapter)

        // Espandi la categoria se unica
        if (categories.size == 1) {
            expandableListView.expandGroup(0)
        }

        // Imposta il listener per il click sui bambini (elementi)
        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            setSelectedProductWithDilution(
                items[groupPosition][childPosition],
                productWithDilutions
            )
            true
        }

        // Crea l'AlertDialog
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Diluizioni per ${productWithDilutions.product.name}")
            .setView(expandableListView)
            .setNegativeButton("Annulla", null)
            .create()

        alertDialog.show()
    }
}
