package com.example.dilutiontool

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ExpandableListView
import android.widget.SearchView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.data.database.AppDatabase
import com.example.dilutiontool.data.database.AppDatabase.Companion.getDatabase
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.Product
import com.example.dilutiontool.entity.ProductWithDilutions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors

class ProductListActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var products: List<ProductWithDilutions>
    private lateinit var filteredProducts: List<ProductWithDilutions>
    private lateinit var productRecyclerView: RecyclerView

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                getProducts()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        val searchView = findViewById<SearchView>(R.id.searchView)
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
                        ) || product.description.contains(newText, ignoreCase = true)
                    }
                    (productRecyclerView.adapter as ProductAdapter).updateList(filteredProducts)
                }
                return true
            }
        })

        val fabAddProduct: FloatingActionButton = findViewById(R.id.fab)

        fabAddProduct.setOnClickListener {
            addProduct()
        }

        db = getDatabase(this)

        getProducts();
    }

    private fun addProduct(productWithDilutions: ProductWithDilutions? = null) {
        val intent = Intent(this, ProductAddActivity::class.java)
        if (productWithDilutions != null) {
            intent.putExtra("PRODUCT_WITH_DILUTIONS", productWithDilutions)
        }
        activityResultLauncher.launch(intent)
    }

    private fun getProducts() {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            //populateDatabase(db)
            products = db.productDao().getAllProductsWithDilutionsSortedByNameAsc()
            filteredProducts = products

            runOnUiThread {
                productRecyclerView = findViewById(R.id.productRecyclerView)
                productRecyclerView.layoutManager = LinearLayoutManager(this@ProductListActivity)
                productRecyclerView.adapter =
                    ProductAdapter(this@ProductListActivity, filteredProducts, { selectedProduct ->
                        // TODO use only one dynamic dialog
                        if (selectedProduct.dilutions.all { it.mode == null })
                            showDilutionSelectionDialog(selectedProduct)
                        else
                            showDialogWithCategorizedItems(selectedProduct)
                    },
                        { selectedProduct ->
                            // Questo è il long click handler
                            val options = arrayOf("Modifica", "Elimina")
                            val builder = android.app.AlertDialog.Builder(this@ProductListActivity)
                            builder.setItems(options) { _, which ->
                                when (which) {
                                    0 -> addProduct(selectedProduct)
                                    1 -> deleteProduct(selectedProduct)
                                }
                            }
                            builder.show()
                            true
                        })
            }
        }
    }

    private fun deleteProduct(productWithDilutions: ProductWithDilutions) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            db.productDao().deleteProductAndDilutions(productWithDilutions)

            runOnUiThread {
                getProducts()
                Toast.makeText(this, "Il prodotto è stato rimosso", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSelectedProductWithDilution(
        selectedDilution: Dilution,
        selectedProductWithDilutions: ProductWithDilutions
    ) {
        val resultIntent = Intent().apply {
            putExtra("selectedDilution", selectedDilution)
            putExtra("selectedProductWithDilutions", selectedProductWithDilutions)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showDilutionSelectionDialog(productWithDilutions: ProductWithDilutions) {
        AlertDialog.Builder(this)
            .setTitle("Diluizioni per ${productWithDilutions.product.name}")
            .setNegativeButton("Annulla", null)
            .setItems(productWithDilutions.dilutions.map { getDescription(it) }
                .toTypedArray()) { _, which ->
                setSelectedProductWithDilution(
                    productWithDilutions.dilutions[which],
                    productWithDilutions
                )
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

    private fun populateDatabase(db: AppDatabase) {
        val productIds = db.productDao().insertProducts(
            listOf(
                Product(
                    name = "LCDA SuperClean",
                    description = "SuperClean è un potente detergente colloidale, ideale per pulire cerchi, motori e superfici delicate come la pelle.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/3658/image_1920/lcdasc-lcda-superclean",
                    link = "https://www.lacuradellauto.it/2763-lcda-superclean"
                ),
                Product(
                    name = "Labocosmetica Semper",
                    description = "Semper è uno shampoo neutro di mantenimento, super concentrato e fortemente lubrificato.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/3701/image_1024/mixlabsem-labocosmetica-semper-shampoo-neutro",
                    link = "https://www.lacuradellauto.it/2784-labocosmetica-semper-shampoo-neutro"
                ),
                Product(
                    name = "Labocosmetica Derma Cleaner",
                    description = "DÈRMA CLEANER 2.0 DI Labocosmetica è semplicemente il prodotto più completo per la cura della pelle",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/3835/image_1024/mixlabder-labocosmetica-derma-cleaner-pulitore-pelle?unique=570e27b",
                    link = "https://www.lacuradellauto.it/2841-labocosmetica-derma-cleaner-pulitore-pelle#attr=2345429"
                ),
                Product(
                    name = "Labocosmetica Primus",
                    description = "PRÌMUS 2.0 di Labocosmetica è un prelavaggio avanzato per auto e moto, migliorato per offrire prestazioni superiori in termini di pulizia e sicurezza.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/4339/image_1024/mixlabpri-labocosmetica-primus-prewash?unique=33ca8ef",
                    link = "https://www.lacuradellauto.it/3034-labocosmetica-primus-prewash#attr=2345706"
                ),
                Product(
                    name = "Labocosmetica Omnia",
                    description = "Omnia è un pulitore per interni auto di nuova generazione, ideale per pulire tessuti, pelle, plastiche, moquette, guarnizioni e gomme, senza rischi per le superfici più delicate.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/3968/image_1024/mixlabom-labocosmetica-omnia-interior-cleaner?unique=422d44d",
                    link = "https://www.lacuradellauto.it/2899-labocosmetica-omnia-interior-cleaner#attr=2345691"
                ),
                Product(
                    name = "Labocosmetica Idrosave",
                    description = "Labocosmetica Idrosave è uno shampoo innovativo che lava, lucida e protegge in un'unica operazione, senza necessità di risciacquo.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/6120/image_1024/mixlabidro-labocosmetica-idrosave-rinseless-waterless-shampoo?unique=a9aadd4",
                    link = "https://www.lacuradellauto.it/4110-labocosmetica-idrosave-rinseless-waterless-shampoo#attr=2346091"
                ),
                Product(
                    name = "Labocosmetica Energo",
                    description = "Energo è un prodotto specializzato nella rimozione di tracce di calcare e residui di piogge acide da vetri e carrozzeria.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/2681/image_1024/lab08-labocosmetica-energo-decontaminante-calcare-250-ml?unique=860b690",
                    link = "https://www.lacuradellauto.it/1980-labocosmetica-energo-decontaminante-calcare-250-ml"
                ),
                Product(
                    name = "Gyeon Q2M Preserve",
                    description = "Q²M Preserve è un prodotto rapido e semplice per ripristinare finiture interne leggermente sbiadite e proteggere altre superfici dall'usura.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/2897/image_1024/g093-gyeon-q2m-preserve-250-ml?unique=a49fe6b",
                    link = "https://www.lacuradellauto.it/2195-gyeon-q2m-preserve-250-ml"
                ),
                Product(
                    name = "Labocosmetica Purifica",
                    description = "Purifica è il primo shampoo acido al mondo nel settore del car detailing, creato da Labocosmetica.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/4052/image_1024/mixlabpf-labocosmetica-purifica-shampoo-acido-anti-calcare?unique=8dc3aa7",
                    link = "https://www.lacuradellauto.it/2928-labocosmetica-purifica-shampoo-acido-anti-calcare.html"
                ),
                Product(
                    name = "LCDA AriaPura",
                    description = "LCDA Aria Pura è un detergente concentrato multiuso ad elevato potere pulente, ideale per tutte le superfici interne dell'auto. Contiene spore batteriche che continuano a combattere sporco e odori anche dopo la pulizia, neutralizzando odori sgradevoli come quelli di nicotina e ammoniaca.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/3655/image_1024/lcdaap-lcda-ariapura?unique=07dd277",
                    link = "https://www.lacuradellauto.it/2762-lcda-ariapura.html"
                ),
                Product(
                    name = "Good Stuff Sour Shampoo",
                    description = "Sour Shampoo di Good Stuff è uno shampoo a pH acido (3,5) progettato per rimuovere contaminazioni minerali che riducono la brillantezza della carrozzeria e l'efficacia dei protettivi. Pulisce a fondo senza cere o polimeri, riattivando le protezioni applicate e rispettando tutte le superfici dell'auto.",
                    imageUrl = "https://www.lacuradellauto.it/web/image/product.product/4215/image_1024/mixgsss-good-stuff-sour-shampoo?unique=07dd277",
                    link = "https://www.lacuradellauto.it/2971-good-stuff-sour-shampoo"
                ),
            )
        )

        db.productDao().insertDilutions(
            listOf(
                Dilution(
                    productId = productIds[0],
                    description = "Sporco grave",
                    value = 5,
                    minValue = 1
                ),
                Dilution(productId = productIds[0], description = "Sporco medio", value = 10, minValue = 10),
                Dilution(productId = productIds[0], description = "Sporco leggero", value = 20, minValue = 20),

                Dilution(productId = productIds[1], description = "Sporco ostinato", value = 800, minValue = 800),
                Dilution(productId = productIds[1], description = "Sporco medio", value = 1000, minValue = 1000),
                Dilution(productId = productIds[1], description = "Sporco leggero", value = 1200, minValue = 1200),

                Dilution(productId = productIds[2], description = "Pulizia speciale", value = 0, minValue = 0),
                Dilution(productId = productIds[2], description = "Pulizia ordinaria", value = 1, minValue = 1),

                Dilution(
                    productId = productIds[3],
                    description = "Cerchi e gomme",
                    value = 10,
                    minValue = 10,
                    mode = "Spray"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Insetti e parte bassa/più sporca",
                    value = 20,
                    minValue = 20,
                    mode = "Spray"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Auto molto sporca",
                    value = 50,
                    minValue = 50,
                    mode = "Spray"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Auto mediamente sporca",
                    value = 80,
                    minValue = 80,
                    mode = "Spray"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Lavaggi frequenti",
                    value = 100,
                    minValue = 100,
                    mode = "Spray"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Sporco invernale o più ostinato",
                    value = 5,
                    minValue = 5,
                    mode = "Foam Gun"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Sporco estivo e mantenimento",
                    value = 10,
                    minValue = 10,
                    mode = "Foam Gun"
                ),
                Dilution(
                    productId = productIds[3],
                    description = "Come shampoo per condizioni di sporco ostinato",
                    value = 100,
                    minValue = 100,
                    mode = "Secchio"
                ),

                Dilution(
                    productId = productIds[4],
                    description = "Per sporchi difficili",
                    value = 5,
                    minValue = 5,
                ),
                Dilution(
                    productId = productIds[4],
                    description = "Come Quick Interior Detailer",
                    value = 10,
                    minValue = 10
                ),

                Dilution(productId = productIds[5], description = "Rinseless", value = 250, minValue = 250),
                Dilution(
                    productId = productIds[5],
                    description = "Waterless o come aiuto all’asciugatura",
                    value = 100,
                    minValue = 100
                ),

                Dilution(
                    productId = productIds[6],
                    description = "Diluito da puro (per casi molto gravi) fino a 1:5",
                    value = 5,
                    minValue = 0
                ),

                Dilution(
                    productId = productIds[7],
                    value = 5,
                    minValue = 0
                ),

                Dilution(
                    productId = productIds[8],
                    description = "Con Foam Gun",
                    value = 10,
                    minValue = 5
                ),
                Dilution(
                    productId = productIds[8],
                    description = "In secchio",
                    value = 400,
                    minValue = 100
                ),
                Dilution(
                    productId = productIds[8],
                    description = "In nebulizzatore",
                    value = 200,
                    minValue = 100
                ),

                Dilution(
                    productId = productIds[9],
                    description = "Sporco grave",
                    value = 5,
                    minValue = 1
                ),
                Dilution(
                    productId = productIds[9],
                    description = "Sporco medio",
                    value = 10,
                    minValue = 5
                ),
                Dilution(
                    productId = productIds[9],
                    description = "Sporco leggero e pulizia di mantenimento",
                    value = 10,
                    minValue = 10
                ),

                Dilution(
                    productId = productIds[10],
                    value = 500,
                    minValue = 160
                ),
            )
        )
    }
}
