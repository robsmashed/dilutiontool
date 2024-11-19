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
    fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProducts(products: List<Product>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDilutions(dilutions: List<Dilution>)

    @Query("SELECT * FROM product_table")
    fun getAllProducts(): List<Product>

    @Transaction
    @Query("SELECT * FROM product_table")
    fun getAllProductsWithDilutions(): List<ProductWithDilutions>

    @Transaction
    @Query("SELECT * FROM product_table ORDER BY name COLLATE NOCASE ASC")
    fun getAllProductsWithDilutionsSortedByNameAsc(): List<ProductWithDilutions>

    @Transaction
    @Query("SELECT * FROM product_table ORDER BY name COLLATE NOCASE DESC")
    fun getAllProductsWithDilutionsSortedByNameDesc(): List<ProductWithDilutions>

    @Query("SELECT COUNT(*) FROM product_table")
    fun getProductCount(): Int

    @Transaction
    @Query("DELETE FROM dilution_table WHERE productId = :productId")
    fun deleteDilutionsForProduct(productId: Long)

    @Transaction
    @Query("DELETE FROM product_table WHERE id = :productId")
    fun deleteProduct(productId: Long)

    @Transaction
    fun deleteProductAndDilutions(productWithDilution: ProductWithDilutions) {
        deleteDilutionsForProduct(productWithDilution.product.id)
        deleteProduct(productWithDilution.product.id)
    }
}
