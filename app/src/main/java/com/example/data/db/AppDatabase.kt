package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Transaction::class,
        TransactionItem::class,
        StoreProfile::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun storeProfileDao(): StoreProfileDao

    companion object {
        private const val DATABASE_NAME = "notapos_database"

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_profile ADD COLUMN themeOption TEXT NOT NULL DEFAULT 'SAME_AS_DEVICE'")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    if (database.productDao().getCount() == 0) {
                                        database.productDao().insertAll(getInitialProducts())
                                    }
                                    if (database.storeProfileDao().getStoreProfile() == null) {
                                        database.storeProfileDao().upsertStoreProfile(StoreProfile())
                                    }
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInitialProducts(): List<Product> {
            return listOf(
                Product(name = "Kopi Susu Gula Aren", sku = "MNM-001", category = "Minuman", costPrice = 8000.0, sellingPrice = 18000.0, stock = 85, unit = "cup"),
                Product(name = "Es Teh Manis Jumbo", sku = "MNM-002", category = "Minuman", costPrice = 2000.0, sellingPrice = 6000.0, stock = 120, unit = "cup"),
                Product(name = "Mie Goreng Spesial Telur", sku = "MKN-001", category = "Makanan", costPrice = 9000.0, sellingPrice = 16000.0, stock = 50, unit = "porsi"),
                Product(name = "Nasi Ayam Geprek Sambal", sku = "MKN-002", category = "Makanan", costPrice = 12000.0, sellingPrice = 22000.0, stock = 40, unit = "porsi"),
                Product(name = "Minyak Goreng 2 Liter", sku = "SBK-001", category = "Sembako", costPrice = 31000.0, sellingPrice = 35000.0, stock = 30, unit = "pouch"),
                Product(name = "Beras Premium 5 Kg", sku = "SBK-002", category = "Sembako", costPrice = 68000.0, sellingPrice = 75000.0, stock = 25, unit = "karung"),
                Product(name = "Gula Pasir 1 Kg", sku = "SBK-003", category = "Sembako", costPrice = 14500.0, sellingPrice = 17500.0, stock = 45, unit = "kg"),
                Product(name = "Telur Ayam 1 Kg", sku = "SBK-004", category = "Sembako", costPrice = 26000.0, sellingPrice = 29500.0, stock = 60, unit = "kg"),
                Product(name = "Keripik Singkong Balado", sku = "SNK-001", category = "Snack", costPrice = 7000.0, sellingPrice = 12000.0, stock = 35, unit = "bungkus"),
                Product(name = "Air Mineral 600ml", sku = "MNM-003", category = "Minuman", costPrice = 2200.0, sellingPrice = 4000.0, stock = 150, unit = "botol")
            )
        }
    }
}
