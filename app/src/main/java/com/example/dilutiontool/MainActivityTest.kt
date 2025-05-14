package com.example.dilutiontool

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivityTest : AppCompatActivity() {

    private lateinit var recyclerTop: RecyclerView

    private val topItems = mutableListOf("Item 1", "Item 2", "Item 3", "Item 4")

    private lateinit var topAdapter: DraggableAdapter

    class NoScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun canScrollVertically(): Boolean {
            return false // Disabilita lo scroll verticale
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_test)

        recyclerTop = findViewById(R.id.recyclerTop)


        // Inizializza l'adapter
        topAdapter = DraggableAdapter(topItems)

        // Crea un callback per il touch helper
        val callbackTop = DragManageAdapter(topAdapter, topItems)

        // Inizializza l'ItemTouchHelper
        val itemTouchHelper = ItemTouchHelper(callbackTop)

        // Associa l'ItemTouchHelper all'adapter
        topAdapter.setTouchHelper(itemTouchHelper)

        // Imposta l'adapter al RecyclerView e il layout manager
        recyclerTop.apply {
            layoutManager = NoScrollLinearLayoutManager(this@MainActivityTest)
            adapter = topAdapter
        }

        // Attacca l'ItemTouchHelper al RecyclerView
        itemTouchHelper.attachToRecyclerView(recyclerTop)
    }
}
