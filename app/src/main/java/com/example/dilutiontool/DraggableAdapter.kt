package com.example.dilutiontool

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DraggableAdapter(
    private val items: MutableList<Item>
) : RecyclerView.Adapter<DraggableAdapter.ViewHolder>() {

    private var touchHelper: ItemTouchHelper? = null

    fun setTouchHelper(helper: ItemTouchHelper) {
        touchHelper = helper
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.title.text = currentItem.label
        holder.valueSuffix.text = currentItem.valueSuffix
        holder.phase.text = getPhaseLabelForPosition(position)
    }

    fun getPhaseLabelForPosition(position: Int): String {
        return when (position) {
            0 -> "FASE 1"
            1 -> "FASE 2"
            else -> "RISULTATO"
        }
    }

    override fun getItemCount(): Int = items.size

    fun moveItem(from: Int, to: Int) {
        val movedItem = items.removeAt(from)
        items.add(to, movedItem)
        notifyItemMoved(from, to)
    }

    fun removeItem(position: Int): Item {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun addItem(item: Item, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val handle: ImageView = itemView.findViewById(R.id.handle)
        val title: TextView = itemView.findViewById(R.id.title)
        val valueSuffix: TextView = itemView.findViewById(R.id.valueSuffix)
        val phase: TextView = itemView.findViewById(R.id.phase)

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
        }
    }
}
