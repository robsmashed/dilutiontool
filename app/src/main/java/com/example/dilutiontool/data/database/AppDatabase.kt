package com.example.dilutiontool.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dilutiontool.dao.ProductDao
import com.example.dilutiontool.entity.Dilution
import com.example.dilutiontool.entity.Product
import java.util.concurrent.Executors

@Database(entities = [Product::class, Dilution::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    // Esegui il popolamento in un thread separato
                    Executors.newSingleThreadExecutor().execute {
                        populateDatabase(database.productDao())
                    }
                }
            }
        }

        fun populateDatabase(productDao: ProductDao) {
            if (productDao.getProductCount() == 0) { // Verifica se il database è vuoto prima di inserire i dati // TODO don't rely on that
                productDao.insertProducts(listOf(
                    Product(
                        2763,
                        "LCDA SuperClean",
                        "Un detergente talmente potente da poter essere utilizzato puro per la pulizia di cerchi e motore e, allo stesso tempo, estremamente delicato da poter essere usato sulla pelle più delicata. Questo è SuperClean!",
                        "https://www.lacuradellauto.it/web/image/product.product/3658/image_1920/lcdasc-lcda-superclean",
                        "https://www.lacuradellauto.it/2763-lcda-superclean"
                    ),
                    Product(
                        2784,
                        "Labocosmetica Semper Shampoo Neutro",
                        "Semper è uno shampoo neutro di mantenimento, super concentrato e fortemente lubrificato.",
                        "https://www.lacuradellauto.it/web/image/product.product/3701/image_1024/mixlabsem-labocosmetica-semper-shampoo-neutro",
                        "https://www.lacuradellauto.it/2784-labocosmetica-semper-shampoo-neutro"
                    ),
                    Product(
                        2841,
                        "Labocosmetica Derma Cleaner - Pulitore Pelle",
                        "DÈRMA CLEANER 2.0 DI Labocosmetica è semplicemente il prodotto più completo per la cura della pelle",
                        "https://www.lacuradellauto.it/web/image/product.product/3835/image_1024/mixlabder-labocosmetica-derma-cleaner-pulitore-pelle?unique=570e27b",
                        "https://www.lacuradellauto.it/2841-labocosmetica-derma-cleaner-pulitore-pelle#attr=2345429"
                    ),
                    Product(
                        3034,
                        "Labocosmetica Primus Prewash",
                        "PRÌMUS 2.0 di Labocosmetica è un prelavaggio avanzato per auto e moto, migliorato per offrire prestazioni superiori in termini di pulizia e sicurezza.",
                        "https://www.lacuradellauto.it/web/image/product.product/4339/image_1024/mixlabpri-labocosmetica-primus-prewash?unique=33ca8ef",
                        "https://www.lacuradellauto.it/3034-labocosmetica-primus-prewash#attr=2345706"
                    ),
                    Product(
                        2899,
                        "Labocosmetica Omnia Interior Cleaner",
                        "Omnia è un pulitore per interni auto di nuova generazione, ideale per pulire tessuti, pelle, plastiche, moquette, guarnizioni e gomme, senza rischi per le superfici più delicate.",
                        "https://www.lacuradellauto.it/web/image/product.product/3968/image_1024/mixlabom-labocosmetica-omnia-interior-cleaner?unique=422d44d",
                        "https://www.lacuradellauto.it/2899-labocosmetica-omnia-interior-cleaner#attr=2345691"
                    ),
                    Product(
                        4110,
                        "Labocosmetica Idrosave Rinseless/Waterless Shampoo",
                        "Labocosmetica Idrosave è uno shampoo innovativo che lava, lucida e protegge in un'unica operazione, senza necessità di risciacquo.",
                        "https://www.lacuradellauto.it/web/image/product.product/6120/image_1024/mixlabidro-labocosmetica-idrosave-rinseless-waterless-shampoo?unique=a9aadd4",
                        "https://www.lacuradellauto.it/4110-labocosmetica-idrosave-rinseless-waterless-shampoo#attr=2346091"
                    ),
                    Product(
                        1980,
                        "Labocosmetica Energo Decontaminante Calcare",
                        "Energo è un prodotto specializzato nella rimozione di tracce di calcare e residui di piogge acide da vetri e carrozzeria.",
                        "https://www.lacuradellauto.it/web/image/product.product/2681/image_1024/lab08-labocosmetica-energo-decontaminante-calcare-250-ml?unique=860b690",
                        "https://www.lacuradellauto.it/1980-labocosmetica-energo-decontaminante-calcare-250-ml"
                    ),
                    Product(
                        2195,
                        "Gyeon Q2M Preserve",
                        "Q²M Preserve è un prodotto rapido e semplice per ripristinare finiture interne leggermente sbiadite e proteggere altre superfici dall'usura.",
                        "https://www.lacuradellauto.it/web/image/product.product/2897/image_1024/g093-gyeon-q2m-preserve-250-ml?unique=a49fe6b",
                        "https://www.lacuradellauto.it/2195-gyeon-q2m-preserve-250-ml"
                    ),
                    Product(
                        2928,
                        "Labocosmetica Purifica Shampoo Acido Anti-Calcare",
                        "Purifica è il primo shampoo acido al mondo nel settore del car detailing, creato da Labocosmetica.",
                        "https://www.lacuradellauto.it/web/image/product.product/4052/image_1024/mixlabpf-labocosmetica-purifica-shampoo-acido-anti-calcare?unique=8dc3aa7",
                        "https://www.lacuradellauto.it/2928-labocosmetica-purifica-shampoo-acido-anti-calcare.html"
                    ),
                ))
                productDao.insertDilutions(listOf(
                    Dilution(productId = 2763, description = "Sporco grave", value = 5, minValue = 1),
                    Dilution(productId = 2763, description = "Sporco medio", value = 10),
                    Dilution(productId = 2763, description = "Sporco leggero", value = 20),

                    Dilution(productId = 2784, description = "Sporco ostinato", value = 800),
                    Dilution(productId = 2784, description = "Sporco medio", value = 1000),
                    Dilution(productId = 2784, description = "Sporco leggero", value = 1200),

                    Dilution(productId = 2841, description = "Pulizia speciale", value = 0),
                    Dilution(productId = 2841, description = "Pulizia ordinaria", value = 1),

                    Dilution(productId = 3034, description = "Cerchi e gomme", value = 10, minValue = null, mode = "Spray"),
                    Dilution(productId = 3034, description = "Insetti e parte bassa/più sporca", value = 20, minValue = null, mode = "Spray"),
                    Dilution(productId = 3034, description = "Auto molto sporca", value = 50, minValue = null, mode = "Spray"),
                    Dilution(productId = 3034, description = "Auto mediamente sporca", value = 80, minValue = null, mode = "Spray"),
                    Dilution(productId = 3034, description = "Lavaggi frequenti", value = 100, minValue = null, mode = "Spray"),
                    Dilution(productId = 3034, description = "Sporco invernale o più ostinato", value = 5, minValue = null, mode = "Foam Gun"),
                    Dilution(productId = 3034, description = "Sporco estivo e mantenimento", value = 10, minValue = null, mode = "Foam Gun"),
                    Dilution(productId = 3034, description = "Come shampoo per condizioni di sporco ostinato", value = 100, minValue = null, mode = "Secchio"),

                    Dilution(productId = 2899, description = "Per sporchi difficili", value = 5),
                    Dilution(productId = 2899, description = "Come Quick Interior Detailer", value = 10),


                    Dilution(productId = 4110, description = "Rinseless", value = 250),
                    Dilution(productId = 4110, description = "Waterless o come Aiuto all’Asciugatura", value = 100),

                    Dilution(productId = 1980, description = "Diluito da puro (per casi molto gravi) fino a 1:5", value = 5, minValue = 0),

                    Dilution(productId = 2195, description = "Diluire fino a 1:5", value = 5, minValue = 0),

                    Dilution(productId = 2928, description = "Con Foam Gun", value = 10, minValue = 5),
                    Dilution(productId = 2928, description = "In secchio", value = 400, minValue = 100),
                    Dilution(productId = 2928, description = "In nebulizzatore", value = 200, minValue = 100),
                ))
            }
        }
    }
}