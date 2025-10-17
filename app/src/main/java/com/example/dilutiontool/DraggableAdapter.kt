package com.example.dilutiontool

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class DraggableAdapter(
    private val items: MutableList<Item>
) : RecyclerView.Adapter<DraggableAdapter.ViewHolder>() {
    var onDilutionRatioChange: ((Double) -> Unit)? = null
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

        // focus & open editor just like you clicked on the edittext
        holder.itemView.setOnClickListener {
            holder.valueEditText.requestFocus()
            val imm = holder.itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(holder.valueEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        // lose focus on keyboard 'done' press
        holder.valueEditText.setOnEditorActionListener { editText, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = holder.itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(holder.valueEditText.windowToken, 0)
                holder.valueEditText.clearFocus()
                true
            } else {
                false
            }
        }

        // Carica l'immagine corrispondente e il gradiente e combina con LayerDrawable
        if (currentItem.bgResId > 0) {
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

    fun getIsEnabledForPosition(position: Int): Boolean {
        return when (position) {
            0 -> true
            1 -> true
            else -> false
        }
    }

    fun getIsEnabled(item: Item): Boolean {
        return getIsEnabledForPosition(items.indexOfFirst { it.id == item.id })
    }

    fun getEditVisibilityForPosition(position: Int): Int {
        return if (getIsEnabledForPosition(position)) View.VISIBLE else View.GONE
    }

    fun getReadVisibilityForPosition(position: Int): Int {
        return if (!getIsEnabledForPosition(position)) View.VISIBLE else View.GONE
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

                item.value = getDoubleValue(valueEditText)
                valueTextView.text = valueEditText.text

                calculateResult(item.id)

                // Always update seekbar with current dilution value
                val dilutionItem = items.find { it.id == ItemId.DILUTION }
                if (dilutionItem != null) {
                    onDilutionRatioChange?.invoke(dilutionItem.value)
                }
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

            valueEditText.setSelectAllOnFocus(true)
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

    fun calculateResult(currentItemId: ItemId, customValue: Double? = null) {
        val totalLiquidIndex = items.indexOfFirst { it.id == ItemId.QUANTITY }
        val dilutionRatioIndex = items.indexOfFirst { it.id == ItemId.DILUTION }
        val waterIndex = items.indexOfFirst { it.id == ItemId.WATER }
        val concentrateIndex = items.indexOfFirst { it.id == ItemId.CONCENTRATE }

        val totalLiquid = items[totalLiquidIndex]
        val dilutionRatio = items[dilutionRatioIndex]
        val water = items[waterIndex]
        val concentrate = items[concentrateIndex]

        if (customValue !== null) {
            items.find { it.id == currentItemId }?.value = customValue
        }

        if (getIsEnabledForPosition(totalLiquidIndex) && getIsEnabledForPosition(dilutionRatioIndex)) {
            concentrate.value = totalLiquid.value / (dilutionRatio.value + 1)
            water.value = totalLiquid.value - concentrate.value
        } else if (getIsEnabledForPosition(totalLiquidIndex) && getIsEnabledForPosition(waterIndex)) {
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
        } else if (getIsEnabledForPosition(totalLiquidIndex) && getIsEnabledForPosition(concentrateIndex)) {
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
        } else if (getIsEnabledForPosition(dilutionRatioIndex) && getIsEnabledForPosition(waterIndex)) {
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
        } else if (getIsEnabledForPosition(dilutionRatioIndex) && getIsEnabledForPosition(concentrateIndex)) {
            if (currentItemId === ItemId.CONCENTRATE && dilutionRatio.value == Double.POSITIVE_INFINITY) {
                concentrate.value = 0.0
                notifyItemChanged(concentrateIndex) // force update yourself
                // TODO avvisare l'utente che la quantità di concentrato rimane 0 a causa del rapporto di diluzione infinito
            } else {
                totalLiquid.value = concentrate.value * (dilutionRatio.value + 1)
                water.value = totalLiquid.value - concentrate.value
            }
        } else if (getIsEnabledForPosition(concentrateIndex) && getIsEnabledForPosition(waterIndex)) {
            totalLiquid.value = concentrate.value + water.value
            val currentDilutionRatio = water.value / concentrate.value
            dilutionRatio.value = if (currentDilutionRatio.isNaN()) 0.0 else currentDilutionRatio
        }

        if (customValue !== null) {
            notifyDataSetChanged() // Update changed value and everything else
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
