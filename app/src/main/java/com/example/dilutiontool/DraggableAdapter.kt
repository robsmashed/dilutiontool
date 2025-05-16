package com.example.dilutiontool

import android.animation.ObjectAnimator
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
    var enableProductSelection: ((Boolean) -> Unit)? = null
    private var touchHelper: ItemTouchHelper? = null

    fun setTouchHelper(helper: ItemTouchHelper) {
        touchHelper = helper
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.title.text = currentItem.label
        holder.valueSuffix.text = currentItem.valueSuffix
        holder.phase.text = getPhaseLabelForPosition(position)
        holder.valueEditText.isEnabled = getEnabledForPosition(position)
        if (currentItem.id === ItemId.DILUTION) {
            enableProductSelection?.invoke(getEnabledForPosition(position))
        }

        holder.valueEditText.removeTextChangedListener(holder.textWatcher)
        holder.valueEditText.setText(getStringValue(currentItem.value))
        holder.valueEditText.addTextChangedListener(holder.textWatcher)

        // Carica l'immagine corrispondente e il gradiente e combina con LayerDrawable
        val context = holder.itemView.context
        val layerDrawable = LayerDrawable(arrayOf(
            ContextCompat.getDrawable(context, currentItem.bgResId),
            ContextCompat.getDrawable(context, R.drawable.fade_gradient)
        ))
        layerDrawable.setLayerGravity(0, Gravity.CENTER)
        holder.itemView.background = layerDrawable
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
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (items[bindingAdapterPosition].id === ItemId.DILUTION) {
                    onDilutionRatioChange?.invoke(valueEditText)
                }

                val item = items[bindingAdapterPosition]
                item.value = getDoubleValue(valueEditText)
                calculateResult(item.id)
                // updateDilutionRangeWarning() TODO
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

    private fun calculateResult(currentItemId: ItemId) {
        val totalLiquidIndex = items.indexOfFirst { it.id == ItemId.QUANTITY }
        val dilutionRatioIndex = items.indexOfFirst { it.id == ItemId.DILUTION }
        val waterIndex = items.indexOfFirst { it.id == ItemId.WATER }
        val concentrateIndex = items.indexOfFirst { it.id == ItemId.CONCENTRATE }

        val totalLiquidItem = items[totalLiquidIndex]
        val dilutionRatioItem = items[dilutionRatioIndex]
        val waterItem = items[waterIndex]
        val concentrateItem = items[concentrateIndex]

        var totalLiquid = totalLiquidItem.value
        var dilutionRatio = dilutionRatioItem.value
        var water = waterItem.value
        var concentrate = concentrateItem.value

        if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(dilutionRatioIndex)) {
            concentrate = totalLiquid / (dilutionRatio + 1)
            water = totalLiquid - concentrate
            concentrateItem.value = concentrate
            waterItem.value = water
        } else if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(waterIndex)) {
            if (totalLiquid - water < 0) {
                if (currentItemId !== ItemId.WATER) {
                    water = totalLiquid
                    waterItem.value = totalLiquid
                } else if (currentItemId !== ItemId.QUANTITY) {
                    totalLiquid = water
                    totalLiquidItem.value = water
                }
            }

            concentrate = totalLiquid - water

            dilutionRatio = if (concentrate == totalLiquid) {
                0.0
            } else if (concentrate == 0.0) {
                Double.POSITIVE_INFINITY
            } else {
                water / concentrate
            }
            concentrateItem.value = concentrate
            dilutionRatioItem.value = dilutionRatio
        } else if (getEnabledForPosition(totalLiquidIndex) && getEnabledForPosition(concentrateIndex)) {
            if (totalLiquid - concentrate < 0) {
                if (currentItemId !== ItemId.QUANTITY) {
                    totalLiquid = concentrate
                    totalLiquidItem.value = concentrate
                } else if (currentItemId !== ItemId.CONCENTRATE) {
                    concentrate = totalLiquid
                    concentrateItem.value = totalLiquid
                }
            }

            water = totalLiquid - concentrate
            dilutionRatio = if (totalLiquid == 0.0 && concentrate == 0.0) 0.0 else totalLiquid / concentrate - 1
            waterItem.value = water
            dilutionRatioItem.value = dilutionRatio
        } else if (getEnabledForPosition(dilutionRatioIndex) && getEnabledForPosition(waterIndex)) {
            if (dilutionRatio == 0.0) {
                if (currentItemId !== ItemId.WATER) {
                    water = 0.0
                    waterItem.value = water
                    concentrate = totalLiquid
                    concentrateItem.value = totalLiquid
                } else if (currentItemId !== ItemId.DILUTION) {
                    water = 0.0
                    waterItem.value = water
                    //waterEditText.selectAll() TODO
                    //flashView(dilutionRatioEditText) TODO
                }
            } else {
                totalLiquid = (water / dilutionRatio) + water
                concentrate = totalLiquid - water
                totalLiquidItem.value = totalLiquid
                concentrateItem.value = concentrate
            }
        } else if (getEnabledForPosition(dilutionRatioIndex) && getEnabledForPosition(concentrateIndex)) {
            if (dilutionRatio == Double.POSITIVE_INFINITY && currentItemId !== ItemId.DILUTION) {
                concentrate = 0.0
                concentrateItem.value = concentrate
                //concentrateEditText.selectAll() TODO
                //flashView(dilutionRatioEditText) TODO
            } else {
                totalLiquid = concentrate * (dilutionRatio + 1)
                water = totalLiquid - concentrate
                totalLiquidItem.value = totalLiquid
                waterItem.value = water
            }
        } else if (getEnabledForPosition(concentrateIndex) && getEnabledForPosition(waterIndex)) {
            totalLiquid = concentrate + water
            dilutionRatio = water / concentrate

            if (dilutionRatio.isNaN()) {
                dilutionRatio = 0.0
            }

            totalLiquidItem.value = totalLiquid
            dilutionRatioItem.value = dilutionRatio
        }

        // TODO
        //notifyItemChanged(0)
        //notifyItemChanged(1)
        //notifyItemChanged(2)
        //notifyItemChanged(3)

        notifyDataSetChanged()
    }

    private fun flashView(view: View) {
        val animation = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f)
        animation.duration = 500 // Durata di ogni ciclo
        animation.repeatCount = 1
        animation.start()
    }

    private fun getDoubleValue(editText: EditText): Double {
        var value = editText.text.toString().toDoubleOrNull() ?: 0.0
        if (editText.text.toString().trim() == "∞") {
            value = Double.POSITIVE_INFINITY
        }
        return value;
    }
}
