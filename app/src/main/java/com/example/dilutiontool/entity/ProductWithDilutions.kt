package com.example.dilutiontool.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ProductWithDilutions(
    @Embedded val product: Product,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val dilutions: List<Dilution>
)
