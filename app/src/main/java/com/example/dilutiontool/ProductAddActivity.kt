package com.example.dilutiontool

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.dilutiontool.data.database.AppDatabase
import com.example.dilutiontool.data.database.AppDatabase.Companion.getDatabase
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.Product
import com.example.dilutiontool.entity.ProductWithDilutions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.Executors

class ProductAddActivity : AppCompatActivity() {
    val PICK_IMAGE_REQUEST = 1
    private lateinit var productNameInput: EditText
    private lateinit var productDescriptionInput: EditText
    private lateinit var productLinkInput: EditText
    private lateinit var dilutionGroups: LinearLayout
    private lateinit var addDilutionGroupButton: Button
    private lateinit var saveProductButton: Button
    private lateinit var productImageView: ImageView
    private var initialProductWithDilutions: ProductWithDilutions? = null
    private var currentProductId: Long = 0
    private var imageBytes: ByteArray? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    setImage(contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    })
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setImage(image: ByteArray?) {
        imageBytes = image
        Glide.with(this)
            .load(image)
            .placeholder(R.drawable.product_loading)
            .error(R.drawable.product_loading)
            .into(productImageView)
    }

    private suspend fun getImageFromUrl(imageUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val output = ByteArrayOutputStream()

                url.openStream().use { stream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int

                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
                output.toByteArray()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        productNameInput = findViewById(R.id.productNameInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productLinkInput = findViewById(R.id.productLinkInput)
        dilutionGroups = findViewById(R.id.dilutionGroups)
        addDilutionGroupButton = findViewById(R.id.addDilutionGroupButton)
        saveProductButton = findViewById(R.id.saveProductButton)
        productImageView = findViewById(R.id.productImageView)

        productImageView.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setItems(arrayOf(
                "Scegli dalla Galleria",
                "Carica da URL"
            )) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        intent.type = "image/*"
                        startActivityForResult(intent, PICK_IMAGE_REQUEST)
                    }
                    1 -> {
                        val editText = EditText(this)
                        editText.hint = "Inserisci URL immagine"
                        val dialog = AlertDialog.Builder(this)
                            .setTitle("Inserisci URL")
                            .setView(editText)
                            .setPositiveButton("Carica", null)
                            .create()
                        dialog.setOnShowListener {
                            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            button.setOnClickListener {
                                val imageUrl = editText.text.toString().trim()
                                if (imageUrl.isNotEmpty()) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        setImage(getImageFromUrl(imageUrl))
                                    }
                                    dialog.dismiss() // Chiudi solo se il campo non è vuoto
                                } else {
                                    editText.error = "Inserisci un URL valido"
                                }
                            }
                        }

                        dialog.show()

                    }
                }
            }
            builder.show()
        }

        val productWithDilutions = getProductWithDilutionsToEdit()
        if (productWithDilutions != null) {
            currentProductId = productWithDilutions.product.id
            productNameInput.setText(productWithDilutions.product.name)
            productDescriptionInput.setText(productWithDilutions.product.description)
            productLinkInput.setText(productWithDilutions.product.link)
            setImage(productWithDilutions.product.image)

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

        initialProductWithDilutions = getCurrentProductWithDilutions()

        saveProductButton.setOnClickListener {
            val currentProductWithDilutions = getCurrentProductWithDilutions()

            if (currentProductWithDilutions.product.name.isEmpty() && currentProductWithDilutions.dilutions.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome e almeno una diluizione valida", Toast.LENGTH_SHORT).show()
            } else if (currentProductWithDilutions.product.name.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome valido", Toast.LENGTH_SHORT).show()
            } else if (currentProductWithDilutions.dilutions.isEmpty()) {
                Toast.makeText(this, "Inserisci almeno una diluizione valida", Toast.LENGTH_SHORT).show()
            } else {
                saveProductWithDilutions(currentProductWithDilutions)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (initialProductWithDilutions == getCurrentProductWithDilutions()) {
                finish()
            } else {
                AlertDialog.Builder(this@ProductAddActivity)
                    .setTitle("Modifiche non salvate")
                    .setMessage("Ci sono modifiche non salvate, vuoi scartare le modifiche?")
                    .setPositiveButton("Scarta") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun getCurrentProductWithDilutions(): ProductWithDilutions {
        val product = Product(
            id = currentProductId,
            name = productNameInput.text.toString(),
            description = productDescriptionInput.text.toString(),
            image = imageBytes,
            link = productLinkInput.text.toString()
        )

        val dilutions = mutableListOf<Dilution>()
        for (i in 0 until dilutionGroups.childCount) {
            val dilutionGroup = dilutionGroups.getChildAt(i) as LinearLayout
            val dilutionGroupName = dilutionGroup.findViewById<EditText>(R.id.dilutionGroupNameEditText).text.toString()
            val dilutionListLayout = dilutionGroup.findViewById<LinearLayout>(R.id.dilutionListLayout)

            for (j in 0 until dilutionListLayout.childCount) {
                val dilutionRow = dilutionListLayout.getChildAt(j)
                val dilutionId = dilutionRow.tag as String?

                var dilutionValue = dilutionRow.findViewById<EditText>(R.id.dilutionInput).text.toString().toIntOrNull()
                var dilutionMinValue = dilutionRow.findViewById<EditText>(R.id.minDilutionInput).text.toString().toIntOrNull()
                if (dilutionValue != null || dilutionMinValue != null) {
                    if (dilutionValue != null && dilutionMinValue != null && dilutionMinValue > dilutionValue) {
                        dilutionValue = dilutionMinValue.also { dilutionMinValue = dilutionValue }
                    }
                    val dilution = Dilution(
                        productId = 0, // Lo inseriamo poi in fase di saveProductWithDilutions
                        description = dilutionRow.findViewById<EditText>(R.id.dilutionDescriptionInput).text.toString(),
                        value = dilutionValue ?: dilutionMinValue ?: 0,
                        minValue = dilutionMinValue ?: dilutionValue ?: 0,
                        mode = dilutionGroupName.ifEmpty { null },
                    )
                    dilutions.add(if (dilutionId !== null) dilution.copy(id = dilutionId.toLong()) else dilution)
                }
            }
        }

        return ProductWithDilutions(
            product = product,
            dilutions = dilutions
        )
    }

    private fun getProductWithDilutionsToEdit(): ProductWithDilutions? {
        return intent.getParcelableExtra("PRODUCT_WITH_DILUTIONS")
    }

    private fun saveProductWithDilutions(productWithDilutions: ProductWithDilutions) {
        if (productWithDilutions.product.name.isNotEmpty() && productWithDilutions.dilutions.isNotEmpty()) {
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
                    resultIntent.putExtra("isAdd", getProductWithDilutionsToEdit() === null)
                    setResult(RESULT_OK, resultIntent)  // Imposta il risultato
                    finish()
                }
            }
        }
    }

    private fun addDilutionGroup(groupName: String = "", dilutions: List<Dilution> = emptyList()) {
        val newDilutionsGroup = LayoutInflater.from(this).inflate(R.layout.dilution_group_layout, dilutionGroups, false)
        newDilutionsGroup.findViewById<EditText>(R.id.dilutionGroupNameEditText).setText(groupName)

        newDilutionsGroup.findViewById<Button>(R.id.removeDilutionGroupButton).setOnClickListener {
            dilutionGroups.removeView(newDilutionsGroup)
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

        dilutionGroups.addView(newDilutionsGroup)
    }

    private fun addDilutionRow(dilutionListLayout: LinearLayout, dilution: Dilution? = null) {
        val newDilutionRow = LayoutInflater.from(this).inflate(R.layout.dilution_row_layout, dilutionListLayout, false)

        newDilutionRow.findViewById<Button>(R.id.removeDilutionButton).setOnClickListener {
            dilutionListLayout.removeView(newDilutionRow)
        }

        if (dilution != null) {
            newDilutionRow.tag = dilution.id.toString()
            newDilutionRow.findViewById<EditText>(R.id.minDilutionInput).setText(dilution.minValue.toString())
            newDilutionRow.findViewById<EditText>(R.id.dilutionInput).setText(dilution.value.toString())
            newDilutionRow.findViewById<EditText>(R.id.dilutionDescriptionInput).setText(dilution.description)
        }

        dilutionListLayout.addView(newDilutionRow)
    }
}
