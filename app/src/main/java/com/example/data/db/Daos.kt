package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM products")
    fun observeProductCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET stock = MAX(0, stock - :quantity), updatedAt = :updatedAt WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = :newStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET sellingPrice = :newPrice, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updatePrice(productId: Long, newPrice: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Long>): Int

    @Query("DELETE FROM products WHERE category = :category")
    suspend fun deleteProductsByCategory(category: String): Int

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsWithItemsList(): List<TransactionWithItems>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionWithItemsById(id: Long): TransactionWithItems?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getTransactionByInvoiceNumber(invoiceNumber: String): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayTransactions(startOfDay: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<Transaction>
}

@Dao
interface StoreProfileDao {
    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    fun observeStoreProfile(): Flow<StoreProfile?>

    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    suspend fun getStoreProfile(): StoreProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStoreProfile(profile: StoreProfile)
}
