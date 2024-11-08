package com.example.dilutiontool.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_table")
data class Product(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val imageUrl: String,
    val link: String
)
