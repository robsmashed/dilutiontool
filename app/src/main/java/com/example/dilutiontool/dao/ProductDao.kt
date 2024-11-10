package com.example.dilutiontool.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.Product
import com.example.dilutiontool.entity.ProductWithDilutions

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDilutions(dilutions: List<Dilution>)

    @Query("SELECT * FROM product_table")
    fun getAllProducts(): List<Product>

    @Transaction
    @Query("SELECT * FROM product_table")
    fun getAllProductsWithDilutions(): List<ProductWithDilutions>

    @Transaction
    @Query("SELECT * FROM product_table ORDER BY name ASC")
    fun getAllProductsWithDilutionsSortedByNameAsc(): List<ProductWithDilutions>

    @Transaction
    @Query("SELECT * FROM product_table ORDER BY name DESC")
    fun getAllProductsWithDilutionsSortedByNameDesc(): List<ProductWithDilutions>

    @Query("SELECT COUNT(*) FROM product_table")
    fun getProductCount(): Int
}
