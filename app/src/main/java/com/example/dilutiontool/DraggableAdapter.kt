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
    private val items: MutableList<String>
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
        holder.textView.text = items[position]

        // Cambia lo sfondo dei primi 2 elementi
        if (position == 0 || position == 1) {
            holder.itemView.setBackgroundColor(Color.RED) // Imposta il colore rosso per i primi 2 elementi
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT) // Imposta uno sfondo trasparente per gli altri
        }
    }

    override fun getItemCount(): Int = items.size

    fun moveItem(from: Int, to: Int) {
        val movedItem = items.removeAt(from)
        items.add(to, movedItem)
        notifyItemMoved(from, to)
    }

    fun removeItem(position: Int): String {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun addItem(item: String, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.itemText)
        val handle: ImageView = itemView.findViewById(R.id.handle)

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
