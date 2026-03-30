package com.xakep1c.familyshop.db

import androidx.room.*
import com.xakep1c.familyshop.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    // Списки покупок
    @Query("SELECT * FROM shopping_lists ORDER BY created_at DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingLists(lists: List<ShoppingList>)

    @Query("UPDATE shopping_lists SET is_completed = :isCompleted WHERE id = :listId")
    suspend fun updateListCompletion(listId: String, isCompleted: Boolean)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteShoppingList(listId: String)

    // Элементы списка
    @Query("SELECT * FROM shopping_list_items WHERE list_id = :listId")
    fun getItemsByListId(listId: String): Flow<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_items WHERE list_id = :listId")
    suspend fun getItemsByListIdDirect(listId: String): List<ShoppingListItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItem>)

    @Query("UPDATE shopping_list_items SET is_checked = :isChecked WHERE id = :itemId")
    suspend fun updateItemChecked(itemId: String, isChecked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    // Магазины
    @Query("SELECT * FROM stores")
    fun getAllStores(): Flow<List<Store>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<Store>)

    // Продукты
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)
    
    @Query("SELECT * FROM products WHERE name_ru LIKE '%' || :query || '%' OR name_nl LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<Product>
}
