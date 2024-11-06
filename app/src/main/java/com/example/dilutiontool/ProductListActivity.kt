package com.example.dilutiontool // Assicurati che il pacchetto sia corretto

import android.content.Intent
import android.os.Bundle
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        // TODO manage this with Room DB
        val products = listOf(
            Product(
                2763,
                "LCDA SuperClean",
                "Un detergente talmente potente da poter essere utilizzato puro per la pulizia di cerchi e motore e, allo stesso tempo, estremamente delicato da poter essere usato sulla pelle più delicata. Questo è SuperClean!",
                arrayOf(
                    Dilution("Sporco grave", 5, 1),
                    Dilution("Sporco medio", 10),
                    Dilution("Sporco leggero e pulizia di mantenimento", 20)
                ),
                "https://www.lacuradellauto.it/web/image/product.product/3658/image_1920/lcdasc-lcda-superclean",
                "https://www.lacuradellauto.it/2763-lcda-superclean"
            ),
            Product(
                2784,
                "Labocosmetica Semper Shampoo Neutro",
                "Semper è uno shampoo neutro di mantenimento, super concentrato e fortemente lubrificato.",
                arrayOf(
                    Dilution("Sporco ostinato", 800),
                    Dilution("Sporco medio", 1000),
                    Dilution("Sporco leggero", 1200)
                ),
                "https://www.lacuradellauto.it/web/image/product.product/3701/image_1024/mixlabsem-labocosmetica-semper-shampoo-neutro",
                "https://www.lacuradellauto.it/2784-labocosmetica-semper-shampoo-neutro"
            ),
            Product(
                2841,
                "Labocosmetica Derma Cleaner - Pulitore Pelle",
                "DÈRMA CLEANER 2.0 DI Labocosmetica è semplicemente il prodotto più completo per la cura della pelle",
                arrayOf(
                    Dilution("Pulizia speciale", 0),
                    Dilution("Pulizia ordinaria", 1),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/3835/image_1024/mixlabder-labocosmetica-derma-cleaner-pulitore-pelle?unique=570e27b",
                "https://www.lacuradellauto.it/2841-labocosmetica-derma-cleaner-pulitore-pelle#attr=2345429"
            ),
            Product(
                3034,
                "Labocosmetica Primus Prewash",
                "PRÌMUS 2.0 di Labocosmetica è un prelavaggio avanzato per auto e moto, migliorato per offrire prestazioni superiori in termini di pulizia e sicurezza.",
                arrayOf(
                    Dilution("Cerchi e gomme", 10, null, "Spray"),
                    Dilution("Insetti e parte bassa/più sporca", 20, null, "Spray"),
                    Dilution("Auto molto sporca", 50, null, "Spray"),
                    Dilution("Auto mediamente sporca", 80, null, "Spray"),
                    Dilution("Lavaggi frequenti", 100, null, "Spray"),

                    Dilution("Sporco invernale o più ostinato", 5, null, "Foam Gun"),
                    Dilution("Sporco estivo e mantenimento", 10, null, "Foam Gun"),

                    Dilution("Come shampoo per condizioni di sporco ostinato", 100, null, "Secchio"),

                ),
                "https://www.lacuradellauto.it/web/image/product.product/4339/image_1024/mixlabpri-labocosmetica-primus-prewash?unique=33ca8ef",
                "https://www.lacuradellauto.it/3034-labocosmetica-primus-prewash#attr=2345706"
            ),
            Product(
                2899,
                "Labocosmetica Omnia Interior Cleaner",
                "Omnia è un pulitore per interni auto di nuova generazione, ideale per pulire tessuti, pelle, plastiche, moquette, guarnizioni e gomme, senza rischi per le superfici più delicate.",
                arrayOf(
                    Dilution("Per sporchi difficili", 5),
                    Dilution("Come Quick Interior Detailer", 10),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/3968/image_1024/mixlabom-labocosmetica-omnia-interior-cleaner?unique=422d44d",
                "https://www.lacuradellauto.it/2899-labocosmetica-omnia-interior-cleaner#attr=2345691"
            ),
            Product(
                4110,
                "Labocosmetica Idrosave Rinseless/Waterless Shampoo",
                "Labocosmetica Idrosave è uno shampoo innovativo che lava, lucida e protegge in un'unica operazione, senza necessità di risciacquo.",
                arrayOf(
                    Dilution("Rinseless", 250),
                    Dilution("Waterless o come Aiuto all’Asciugatura", 100),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/6120/image_1024/mixlabidro-labocosmetica-idrosave-rinseless-waterless-shampoo?unique=a9aadd4",
                "https://www.lacuradellauto.it/4110-labocosmetica-idrosave-rinseless-waterless-shampoo#attr=2346091"
            ),
            Product(
                1980,
                "Labocosmetica Energo Decontaminante Calcare",
                "Energo è un prodotto specializzato nella rimozione di tracce di calcare e residui di piogge acide da vetri e carrozzeria.",
                arrayOf(
                    Dilution("Diluito da puro (per casi molto gravi) fino a 1:5", 5, 0),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/2681/image_1024/lab08-labocosmetica-energo-decontaminante-calcare-250-ml?unique=860b690",
                "https://www.lacuradellauto.it/1980-labocosmetica-energo-decontaminante-calcare-250-ml"
            ),
            Product(
                2195,
                "Gyeon Q2M Preserve",
                "Q²M Preserve è un prodotto rapido e semplice per ripristinare finiture interne leggermente sbiadite e proteggere altre superfici dall'usura.",
                arrayOf(
                    Dilution("Diluire fino a 1:5", 5, 0),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/2897/image_1024/g093-gyeon-q2m-preserve-250-ml?unique=a49fe6b",
                "https://www.lacuradellauto.it/2195-gyeon-q2m-preserve-250-ml"
            ),
            Product(
                2928,
                "Labocosmetica Purifica Shampoo Acido Anti-Calcare",
                "Purifica è il primo shampoo acido al mondo nel settore del car detailing, creato da Labocosmetica.",
                arrayOf(
                    Dilution("Con Foam Gun", 10, 5),
                    Dilution("In secchio", 400, 100),
                    Dilution("In nebulizzatore", 200, 100),
                ),
                "https://www.lacuradellauto.it/web/image/product.product/4052/image_1024/mixlabpf-labocosmetica-purifica-shampoo-acido-anti-calcare?unique=8dc3aa7",
                "https://www.lacuradellauto.it/2928-labocosmetica-purifica-shampoo-acido-anti-calcare.html"
            ),
        )

        val productRecyclerView: RecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = LinearLayoutManager(this)
        productRecyclerView.adapter = ProductAdapter(this, products) { selectedProduct ->
            // TODO use only one function
            if (selectedProduct.dilutions.all { it.mode == null })
                showDilutionSelectionDialog(selectedProduct)
            else
                showDialogWithCategorizedItems(selectedProduct)
        }
    }

    private fun getDescription(dilution: Dilution): String {
        var dilutionDescription = "puro"
        if (dilution.value.toInt() > 0) {
            if (dilution.minValue != null) {
                dilutionDescription = "1:${dilution.minValue} - 1:${dilution.value}"
            } else {
                dilutionDescription = "1:${dilution.value}"
            }
        }
        return "${dilution.description} ($dilutionDescription)"
    }

    private fun showDilutionSelectionDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Diluizione per ${product.name}")
            .setNegativeButton("Annulla", null)
            .setItems(product.dilutions.map { getDescription(it) }.toTypedArray()) { _, which ->
                val selectedDilution = product.dilutions[which]
                val resultIntent = Intent().apply {
                    putExtra("selectedDilution", selectedDilution.value)
                    putExtra("productName", product.name)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .show()
    }

    fun showDialogWithCategorizedItems(product: Product) {
        // Definisci le categorie e gli elementi per ciascuna categoria
        val categories = product.dilutions.mapNotNull { it.mode }.distinct()

        val items = product.dilutions.groupBy { it.mode }
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
            .setTitle("Diluizione per ${product.name}")
            .setView(expandableListView)
            .setNegativeButton("Annulla", null)
            .create()

        alertDialog.show()
    }
}
