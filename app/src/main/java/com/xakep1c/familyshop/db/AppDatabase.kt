package com.xakep1c.familyshop.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xakep1c.familyshop.model.*

@Database(
    entities = [
        ShoppingList::class,
        ShoppingListItem::class,
        Store::class,
        Category::class,
        Product::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "family_shop_database"
                )
                .fallbackToDestructiveMigration() // Полезно при разработке, но осторожно в продакшене
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
