package com.xakep1c.familyshop.repository

import com.xakep1c.familyshop.model.Category
import com.xakep1c.familyshop.model.ShoppingList
import com.xakep1c.familyshop.model.ShoppingListItem
import com.xakep1c.familyshop.model.Store
import com.xakep1c.familyshop.supabase
import io.github.jan.supabase.postgrest.postgrest

class ShoppingRepository {

    // Получить все магазины
    suspend fun getStores(): List<Store> =
        supabase.postgrest["stores"].select().decodeList<Store>()

    // Получить все категории
    suspend fun getCategories(): List<Category> =
        supabase.postgrest["categories"].select().decodeList<Category>()

    // Получить все списки покупок
    suspend fun getShoppingLists(): List<ShoppingList> =
        supabase.postgrest["shopping_lists"].select().decodeList<ShoppingList>()

    // Получить элементы конкретного списка
    suspend fun getItems(listId: String): List<ShoppingListItem> =
        supabase.postgrest["shopping_list_items"]
            .select { filter { eq("list_id", listId) } }
            .decodeList<ShoppingListItem>()

    // Добавить новый список
    suspend fun createShoppingList(name: String): ShoppingList =
        supabase.postgrest["shopping_lists"]
            .insert(mapOf("name" to name))
            .decodeSingle<ShoppingList>()

    // Добавить товар в список
    suspend fun addItem(item: ShoppingListItem) {
        supabase.postgrest["shopping_list_items"]
            .insert(item)
    }

    // Отметить товар купленным
    suspend fun checkItem(itemId: String, checked: Boolean) =
        supabase.postgrest["shopping_list_items"]
            .update(mapOf("is_checked" to checked)) {
                filter { eq("id", itemId) }
            }

    // Удалить товар
    suspend fun deleteItem(itemId: String) =
        supabase.postgrest["shopping_list_items"]
            .delete { filter { eq("id", itemId) } }

}