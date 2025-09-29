package com.example.dilutiontool

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragManageAdapter(
    private val adapter: DraggableAdapter,
    private val sourceList: MutableList<Item>,
    private val onDraggingChange: ((Boolean) -> Unit)? = null
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = viewHolder.adapterPosition
        val toPos = target.adapterPosition
        adapter.moveItem(fromPos, toPos)

        for (i in 0 until adapter.itemCount) {
            val vh = recyclerView.findViewHolderForAdapterPosition(i)
            if (vh is DraggableAdapter.ViewHolder) {
                vh.phase.text = adapter.getPhaseLabelForPosition(i)
                vh.valueEditText.visibility = adapter.getEditVisibilityForPosition(i)
                vh.valueTextView.visibility = adapter.getReadVisibilityForPosition(i)
            }
        }

        onDraggingChange?.invoke(adapter.getIsEnabledForPosition(sourceList.indexOfFirst { it.id == ItemId.DILUTION }))

        return true
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return

        // Aggiorna lo sfondo dell'elemento in base alla sua posizione
        // Questo viene eseguito solo quando il drag è completo
        adapter.notifyItemChanged(position)

        // Se vuoi anche aggiornare altri elementi che potrebbero essere stati spostati
        // puoi notificare gli altri oggetti cambiati (come i primi 2)
        recyclerView.adapter?.notifyDataSetChanged()

        val item = sourceList.getOrNull(position) ?: return
    }
}
