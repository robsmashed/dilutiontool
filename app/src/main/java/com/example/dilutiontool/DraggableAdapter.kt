package com.example.dilutiontool

import android.graphics.drawable.LayerDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class DraggableAdapter(private val items: MutableList<Item>) : RecyclerView.Adapter<DraggableAdapter.ViewHolder>() {
    var onDilutionRatioChange: ((EditText) -> Unit)? = null
    var touchHelper: ItemTouchHelper? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.title.text = currentItem.label
        holder.valueSuffix.text = currentItem.valueSuffix
        holder.phase.text = getPhaseLabelForPosition(position)
        holder.valueEditText.visibility = getEditVisibilityForPosition(position)
        holder.valueTextView.visibility = getReadVisibilityForPosition(position)

        holder.valueTextView.text = getStringValue(currentItem.value)

        holder.valueEditText.removeTextChangedListener(holder.textWatcher)
        holder.valueEditText.setText(getStringValue(currentItem.value))
        holder.valueEditText.addTextChangedListener(holder.textWatcher)

        if (currentItem.bgResId > 0) {
            // Carica l'immagine corrispondente e il gradiente e combina con LayerDrawable
            val context = holder.itemView.context
            val layerDrawable = LayerDrawable(arrayOf(
                ContextCompat.getDrawable(context, currentItem.bgResId),
                ContextCompat.getDrawable(context, R.drawable.fade_gradient)
            ))
            layerDrawable.setLayerGravity(0, Gravity.CENTER)
            holder.itemView.background = layerDrawable
        }
    }

    fun getPhaseLabelForPosition(position: Int): String {
        return when (position) {
            0 -> "FASE 1"
            1 -> "FASE 2"
            else -> "RISULTATO"
        }
    }

    fun getEnabledForPosition(position: Int): Boolean {
        return when (position) {
            0 -> true
            1 -> true
            else -> false
        }
    }

    fun getEditVisibilityForPosition(position: Int): Int {
        return if (getEnabledForPosition(position)) View.VISIBLE else View.GONE
    }

    fun getReadVisibilityForPosition(position: Int): Int {
        return if (!getEnabledForPosition(position)) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = items.size

    fun moveItem(from: Int, to: Int) {
        val movedItem = items.removeAt(from)
        items.add(to, movedItem)
        notifyItemMoved(from, to)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val handle: ImageView = itemView.findViewById(R.id.handle)
        val title: TextView = itemView.findViewById(R.id.title)
        val valueSuffix: TextView = itemView.findViewById(R.id.valueSuffix)
        val phase: TextView = itemView.findViewById(R.id.phase)
        val valueEditText: EditText = itemView.findViewById(R.id.valueEditText)
        val valueTextView: TextView = itemView.findViewById(R.id.valueTextView)
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val item = items[bindingAdapterPosition]

                if (item.id === ItemId.DILUTION) {
                    onDilutionRatioChange?.invoke(valueEditText)
                }

                item.value = getDoubleValue(valueEditText)
                valueTextView.text = valueEditText.text

                calculateResult(item.id)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        init {
            handle.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    // Avvia il drag
                    touchHelper?.startDrag(this)
                }

                // Gestisci l'evento di clic (necessario per evitare l'avviso)
                v.performClick()

                false // Restituisci false per non consumare l'evento
            }

            limitDecimalInput(valueEditText)
            valueEditText.addTextChangedListener(textWatcher)

            valueEditText.setSelectAllOnFocus(true);
            valueEditText.setOnClickListener {
                valueEditText.clearFocus()
                valueEditText.requestFocus()
            }
        }
    }

    private fun limitDecimalInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val text = editable.toString()
                if (text.contains(".")) {
                    val parts = text.split(".")
                    if (parts.size == 2 && parts[1].length > 2) {
                        val newText = parts[0] + "." + parts[1].substring(0, 2)
                        editText.setText(newText)
                        editText.setSelection(newText.length)
                    }
                }
            }
        })
    }

    private fun getStringValue(value: Double): String {
        return when (value) {
            Double.POSITIVE_INFINITY -> "∞"  // Se il numero è infinito, ritorna "∞"
            value.toInt().toDouble() -> value.toInt().toString()  // Se il numero è intero, rimuove i decimali
            else -> {
                val formattedValue = String.format(Locale.US, "%.2f", value)
                val formattedValueDouble = formattedValue.toDouble()
                if (formattedValueDouble == formattedValueDouble.toInt().toDouble()) {
                    formattedValueDouble.toInt().toString() // mostra senza decimali
                } else {
                    formattedValue // Mostra un solo decimale con punto
                }
            }
        }
    }

    fun calculateResult(currentItemId: ItemId, notifyDataSetChanged: Boolean = false) {
        val totalLiquidIndex = items.indexOfFirst { it.id == ItemId.QUANTITY }
        val dilutionRatioIndex = items.indexOfFirst { it.id == ItemId.DILUTION }
        val waterIndex = items.indexOfFirst { it.id == ItemId.WATER }
        val concentrateIndex = items.indexOfFirst { it.id == ItemId.CONCENTRATE }

        val totalLiquid = items[totalLiquidIndex]
        val dilutionRatio = items[dilutionRatioIndex]
        val water = items[waterIndex]
        val concentrate = items[concentrateIndex]

        if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(dilutionRatioIndex)) {
            concentrate.value = totalLiquid.value / (dilutionRatio.value + 1)
            water.value = totalLiquid.value - concentrate.value
        } else if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(waterIndex)) {
            if (totalLiquid.value < water.value) {
                if (currentItemId === ItemId.WATER) {
                    totalLiquid.value = water.value
                } else if (currentItemId === ItemId.QUANTITY) {
                    water.value = totalLiquid.value
                    // TODO keep updating water?
                }
            }

            concentrate.value = totalLiquid.value - water.value
            dilutionRatio.value = if (concentrate.value == totalLiquid.value) {
                0.0
            } else if (concentrate.value == 0.0) {
                Double.POSITIVE_INFINITY
            } else {
                water.value / concentrate.value
            }
        } else if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(concentrateIndex)) {
            if (totalLiquid.value < concentrate.value) {
                if (currentItemId === ItemId.QUANTITY) {
                    concentrate.value = totalLiquid.value
                    // TODO keep updating concentrate?
                } else if (currentItemId === ItemId.CONCENTRATE) {
                    totalLiquid.value = concentrate.value
                }
            }

            water.value = totalLiquid.value - concentrate.value
            dilutionRatio.value = if (totalLiquid.value == 0.0 && concentrate.value == 0.0) 0.0 else totalLiquid.value / concentrate.value - 1
        } else if (getEnabledForPosition(dilutionRatioIndex) && getEnabledForPosition(waterIndex)) {
            if (dilutionRatio.value == 0.0) {
                if (currentItemId === ItemId.WATER) {
                    water.value = 0.0
                    notifyItemChanged(waterIndex) // force update yourself
                    // TODO avvisare l'utente che la quantità d'acqua rimane 0 a causa del rapporto di diluzione 0
                } else if (currentItemId === ItemId.DILUTION) {
                    water.value = 0.0
                    concentrate.value = totalLiquid.value
                }
            } else {
                totalLiquid.value = (water.value / dilutionRatio.value) + water.value
                concentrate.value = totalLiquid.value - water.value
            }
        } else if (getEnabledForPosition(dilutionRatioIndex) && getEnabledForPosition(concentrateIndex)) {
            if (currentItemId === ItemId.CONCENTRATE && dilutionRatio.value == Double.POSITIVE_INFINITY) {
                concentrate.value = 0.0
                notifyItemChanged(concentrateIndex) // force update yourself
                // TODO avvisare l'utente che la quantità di concentrato rimane 0 a causa del rapporto di diluzione infinito
            } else {
                totalLiquid.value = concentrate.value * (dilutionRatio.value + 1)
                water.value = totalLiquid.value - concentrate.value
            }
        } else if (getEnabledForPosition(concentrateIndex) && getEnabledForPosition(waterIndex)) {
            totalLiquid.value = concentrate.value + water.value
            val currentDilutionRatio = water.value / concentrate.value
            dilutionRatio.value = if (currentDilutionRatio.isNaN()) 0.0 else currentDilutionRatio
        }

        if (notifyDataSetChanged) {
            notifyDataSetChanged()
        } else {
            // Update everything but changed value
            if (ItemId.QUANTITY !== currentItemId) {
                notifyItemChanged(totalLiquidIndex)
            }
            if (ItemId.DILUTION !== currentItemId) {
                notifyItemChanged(dilutionRatioIndex)
            }
            if (ItemId.WATER !== currentItemId) {
                notifyItemChanged(waterIndex)
            }
            if (ItemId.CONCENTRATE !== currentItemId) {
                notifyItemChanged(concentrateIndex)
            }
        }
        // updateDilutionRangeWarning() TODO
    }

    private fun getDoubleValue(editText: EditText): Double {
        var value = editText.text.toString().toDoubleOrNull() ?: 0.0
        if (editText.text.toString().trim() == "∞") {
            value = Double.POSITIVE_INFINITY
        }
        return value;
    }
}
