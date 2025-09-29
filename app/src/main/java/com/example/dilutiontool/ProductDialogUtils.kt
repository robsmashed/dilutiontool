package com.example.dilutiontool

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import com.example.dilutiontool.DilutionUtils.getDescription
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.ProductWithDilutions

object ProductDialogUtils {

    fun showDilutionDialog(
        context: Context,
        productWithDilutions: ProductWithDilutions,
        onDilutionSelected: (Dilution) -> Unit
    ) {
        val sortedDilutions = productWithDilutions.dilutions.sortedBy { it.minValue }

        if (sortedDilutions.all { it.mode == null }) {
            // lista semplice
            val adapter = ArrayAdapter(
                context,
                R.layout.dialog_list_item,
                sortedDilutions.map { getDescription(it) }
            )

            AlertDialog.Builder(context)
                .setTitle("Diluizioni per ${productWithDilutions.product.name}")
                .setNegativeButton("Annulla", null)
                .setAdapter(adapter) { _, which ->
                    onDilutionSelected(sortedDilutions[which])
                }
                .show()
        } else {
            // lista con categorie
            val categories = sortedDilutions.map { it.mode }.distinct()
            val items = sortedDilutions.groupBy { it.mode }.values.toList()

            val groupData = categories.map { mapOf("CATEGORY" to (it ?: "Categoria non definita")) }
            val childData = items.map { it.map { d -> mapOf("ITEM" to getDescription(d)) } }

            val expandableListAdapter = SimpleExpandableListAdapter(
                context,
                groupData,
                android.R.layout.simple_expandable_list_item_1,
                arrayOf("CATEGORY"),
                intArrayOf(android.R.id.text1),
                childData,
                R.layout.dialog_list_item,
                arrayOf("ITEM"),
                intArrayOf(android.R.id.text1)
            )

            val expandableListView = ExpandableListView(context)
            expandableListView.setAdapter(expandableListAdapter)
            if (categories.size == 1) expandableListView.expandGroup(0)

            val alertDialog = AlertDialog.Builder(context)
                .setTitle("Diluizioni per ${productWithDilutions.product.name}")
                .setView(expandableListView)
                .setNegativeButton("Annulla", null)
                .create()

            expandableListView.setOnChildClickListener { _, _, groupPos, childPos, _ ->
                onDilutionSelected(items[groupPos][childPos])
                alertDialog.dismiss()
                true
            }

            alertDialog.show()
        }
    }
}
