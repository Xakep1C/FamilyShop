package com.xakep1c.familyshop.repository

import com.xakep1c.familyshop.db.ShoppingDao
import com.xakep1c.familyshop.model.*
import com.xakep1c.familyshop.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ShoppingRepository(val dao: ShoppingDao) {

    // --- Списки покупок ---
    val allShoppingLists: Flow<List<ShoppingList>> = dao.getAllShoppingLists()

    suspend fun refreshShoppingLists() = withContext(Dispatchers.IO) {
        try {
            val remoteLists = supabase.postgrest["shopping_lists"].select().decodeList<ShoppingList>()
            dao.insertShoppingLists(remoteLists)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing lists: ${e.message}")
        }
    }

    // Слушаем изменения в таблицах через Supabase Realtime
    fun observeRealtimeChanges(scope: CoroutineScope) {
        val channel = supabase.channel("public-changes")

        // 1. Слушаем изменения в списках
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shopping_lists"
        }.onEach { action ->
            withContext(Dispatchers.IO) {
                try {
                    when (action) {
                        is PostgresAction.Insert -> dao.insertShoppingLists(listOf(action.decodeRecord<ShoppingList>()))
                        is PostgresAction.Update -> dao.insertShoppingLists(listOf(action.decodeRecord<ShoppingList>()))
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.toString()?.replace("\"", "")
                            if (id != null) dao.deleteShoppingList(id)
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("Realtime", "Error decoding list action: ${e.message}")
                }
            }
        }.launchIn(scope)

        // 2. Слушаем изменения в товарах
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shopping_list_items"
        }.onEach { action ->
            withContext(Dispatchers.IO) {
                try {
                    when (action) {
                        is PostgresAction.Insert -> dao.insertItems(listOf(action.decodeRecord<ShoppingListItem>()))
                        is PostgresAction.Update -> dao.insertItems(listOf(action.decodeRecord<ShoppingListItem>()))
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.toString()?.replace("\"", "")
                            if (id != null) dao.deleteItem(id)
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("Realtime", "Error decoding item action: ${e.message}")
                }
            }
        }.launchIn(scope)

        // Подключаемся
        scope.launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                Log.e("Realtime", "Subscription failed: ${e.message}")
            }
        }
    }

    suspend fun createShoppingList(name: String) = withContext(Dispatchers.IO) {
        val localId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        
        val newList = ShoppingList(
            id = localId,
            name = name,
            createdAt = timestamp
        )
        
        // Сначала в локальную БД
        dao.insertShoppingLists(listOf(newList))
        
        try {
            // Пытаемся отправить в Supabase
            val remoteList = supabase.postgrest["shopping_lists"]
                .insert(newList)
                .decodeSingle<ShoppingList>()
            
            // Если сервер вернул более актуальные данные (например, ID или created_at), обновляем локально
            dao.insertShoppingLists(listOf(remoteList))
        } catch (e: Exception) {
            Log.e("Repository", "Sync createShoppingList failed: ${e.message}")
        }
    }

    suspend fun deleteShoppingList(listId: String) = withContext(Dispatchers.IO) {
        dao.deleteShoppingList(listId)
        try {
            supabase.postgrest["shopping_lists"].delete {
                filter { eq("id", listId) }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Delete list sync failed: ${e.message}")
        }
    }

    suspend fun completeShoppingList(listId: String) = withContext(Dispatchers.IO) {
        dao.updateListCompletion(listId, true)
        try {
            supabase.postgrest["shopping_lists"]
                .update(mapOf("is_completed" to true)) {
                    filter { eq("id", listId) }
                }
        } catch (e: Exception) {
            Log.e("Repository", "Error completing list: ${e.message}")
        }
    }

    // --- Элементы списка ---
    fun getItems(listId: String): Flow<List<ShoppingListItem>> = dao.getItemsByListId(listId)

    suspend fun getItemsDirect(listId: String): List<ShoppingListItem> = dao.getItemsByListIdDirect(listId)

    suspend fun refreshItems(listId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteItems = supabase.postgrest["shopping_list_items"]
                .select { filter { eq("list_id", listId) } }
                .decodeList<ShoppingListItem>()
            dao.insertItems(remoteItems)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing items: ${e.message}")
        }
    }

    suspend fun addItem(item: ShoppingListItem) = withContext(Dispatchers.IO) {
        dao.insertItems(listOf(item)) // Optimistic UI
        try {
            supabase.postgrest["shopping_list_items"].insert(item)
        } catch (e: Exception) {
            Log.e("Repository", "Error adding item: ${e.message}")
        }
    }

    suspend fun addItems(items: List<ShoppingListItem>) = withContext(Dispatchers.IO) {
        dao.insertItems(items) // Optimistic UI
        try {
            supabase.postgrest["shopping_list_items"].insert(items)
        } catch (e: Exception) {
            Log.e("Repository", "Error adding items: ${e.message}")
        }
    }

    suspend fun checkItem(itemId: String, checked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateItemChecked(itemId, checked)
        try {
            supabase.postgrest["shopping_list_items"]
                .update(mapOf("is_checked" to checked)) {
                    filter { eq("id", itemId) }
                }
        } catch (e: Exception) {
            Log.e("Repository", "Sync checkItem failed: ${e.message}")
        }
    }

    suspend fun deleteItem(itemId: String) = withContext(Dispatchers.IO) {
        dao.deleteItem(itemId)
        try {
            supabase.postgrest["shopping_list_items"].delete { filter { eq("id", itemId) } }
        } catch (e: Exception) {
             Log.e("Repository", "Delete item sync failed: ${e.message}")
        }
    }

    // --- Магазины и Продукты ---
    val allStores: Flow<List<Store>> = dao.getAllStores()
    val allProducts: Flow<List<Product>> = dao.getAllProducts()

    suspend fun refreshStores() = withContext(Dispatchers.IO) {
        try {
            val remote = supabase.postgrest["stores"].select().decodeList<Store>()
            dao.insertStores(remote)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing stores: ${e.message}")
        }
    }

    suspend fun refreshProducts() = withContext(Dispatchers.IO) {
        try {
            val remote = supabase.postgrest["products"].select().decodeList<Product>()
            dao.insertProducts(remote)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing products: ${e.message}")
        }
    }

    suspend fun addProducts(products: List<Product>) = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest["products"].upsert(products)
            dao.insertProducts(products)
        } catch (e: Exception) {
            Log.e("Repository", "Error adding products: ${e.message}")
        }
    }
}
