package com.xakep1c.familyshop.repository

import com.xakep1c.familyshop.db.ShoppingDao
import com.xakep1c.familyshop.model.*
import com.xakep1c.familyshop.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import android.util.Log

class ShoppingRepository(private val dao: ShoppingDao) {

    // --- Списки покупок ---
    val allShoppingLists: Flow<List<ShoppingList>> = dao.getAllShoppingLists()

    suspend fun refreshShoppingLists() {
        try {
            val remoteLists = supabase.postgrest["shopping_lists"].select().decodeList<ShoppingList>()
            dao.insertShoppingLists(remoteLists)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing lists: ${e.message}")
        }
    }

    suspend fun createShoppingList(name: String) {
        val newList = supabase.postgrest["shopping_lists"]
            .insert(mapOf("name" to name))
            .decodeSingle<ShoppingList>()
        dao.insertShoppingLists(listOf(newList))
    }

    suspend fun completeShoppingList(listId: String) {
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

    suspend fun refreshItems(listId: String) {
        try {
            val remoteItems = supabase.postgrest["shopping_list_items"]
                .select { filter { eq("list_id", listId) } }
                .decodeList<ShoppingListItem>()
            dao.insertItems(remoteItems)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing items: ${e.message}")
        }
    }

    suspend fun addItem(item: ShoppingListItem) {
        supabase.postgrest["shopping_list_items"].insert(item)
        refreshItems(item.listId)
    }

    suspend fun addItems(items: List<ShoppingListItem>) {
        supabase.postgrest["shopping_list_items"].insert(items)
        if (items.isNotEmpty()) {
            refreshItems(items.first().listId)
        }
    }

    suspend fun checkItem(itemId: String, checked: Boolean) {
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

    suspend fun deleteItem(itemId: String) {
        dao.deleteItem(itemId)
        supabase.postgrest["shopping_list_items"].delete { filter { eq("id", itemId) } }
    }

    // --- Магазины и Продукты ---
    val allStores: Flow<List<Store>> = dao.getAllStores()
    val allProducts: Flow<List<Product>> = dao.getAllProducts()

    suspend fun refreshStores() {
        try {
            val remote = supabase.postgrest["stores"].select().decodeList<Store>()
            dao.insertStores(remote)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing stores: ${e.message}")
        }
    }

    suspend fun refreshProducts() {
        try {
            val remote = supabase.postgrest["products"].select().decodeList<Product>()
            dao.insertProducts(remote)
        } catch (e: Exception) {
            Log.e("Repository", "Error refreshing products: ${e.message}")
        }
    }
}
