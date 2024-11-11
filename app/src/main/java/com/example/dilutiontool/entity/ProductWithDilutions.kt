package com.example.dilutiontool.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductWithDilutions(
    @Embedded val product: Product,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val dilutions: List<Dilution>
) : Parcelable
