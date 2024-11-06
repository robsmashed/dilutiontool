package com.example.dilutiontool

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var dilutionRatioEditText: EditText

    // Dichiarazione di ActivityResultLauncher
    private val productSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val dilution = result.data?.getIntExtra("selectedDilution", 0)

            dilutionRatioEditText.setText(dilution.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dilutionRatioEditText = findViewById<EditText>(R.id.dilutionRatio)
        val totalLiquidEditText = findViewById<EditText>(R.id.totalLiquid)
        val resultText = findViewById<TextView>(R.id.resultText)
        val waterResultText = findViewById<TextView>(R.id.waterResultText)
        var selectProductButton = findViewById<TextView>(R.id.selectProductButton)

        selectProductButton.setOnClickListener {
            val intent = Intent(this, ProductListActivity::class.java)
            productSelectionLauncher.launch(intent)
        }

        // Funzione per calcolare e aggiornare il risultato
        fun calculateResult() {
            val dilutionRatio = dilutionRatioEditText.text.toString().toDoubleOrNull()
            val totalLiquid = totalLiquidEditText.text.toString().toDoubleOrNull()

            if (dilutionRatio != null && dilutionRatio > 0 && totalLiquid != null && totalLiquid > 0) {
                val concentrate = totalLiquid / (dilutionRatio + 1)
                val water = totalLiquid - concentrate
                val formattedConcentrate = String.format("%.1f", concentrate)
                val formattedWater = String.format("%.1f", water)
                resultText.text = "$formattedConcentrate ml"
                waterResultText.text = "$formattedWater ml"
            } else {
                resultText.text = ""
                waterResultText.text = ""
            }
        }

        // Aggiungi il TextWatcher agli EditText
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateResult()
            }
        }

        dilutionRatioEditText.addTextChangedListener(textWatcher)
        totalLiquidEditText.addTextChangedListener(textWatcher)
    }
}
