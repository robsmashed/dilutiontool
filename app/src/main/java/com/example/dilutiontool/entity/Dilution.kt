package com.example.dilutiontool.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "dilution_table")
@Parcelize
data class Dilution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int, // Foreign key reference to Product
    val description: String,
    val value: Int,
    val minValue: Int? = null,
    val mode: String? = null
) : Parcelable
