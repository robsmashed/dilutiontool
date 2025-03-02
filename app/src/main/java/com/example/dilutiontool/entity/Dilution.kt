package com.example.dilutiontool.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "dilution_table")
@Parcelize
data class Dilution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, // Foreign key reference to Product
    val description: String? = "",
    val value: Int,
    val minValue: Int,
    val mode: String? = null
) : Parcelable
