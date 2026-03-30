package com.xakep1c.familyshop.model



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Store(
    val id: String = "",
    val name: String = "",
    val color: String = ""
)

@Serializable
data class Category(
    val id: String = "",
    @SerialName("name_ru") val nameRu: String = "",
    @SerialName("name_nl") val nameNl: String = ""
)

@Serializable
data class ShoppingList(
    val id: String = "",
    val name: String = "Список",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ShoppingListItem(
    val id: String = "",
    @SerialName("list_id") val listId: String = "",
    @SerialName("custom_name") val customName: String = "",
    @SerialName("store_id") val storeId: String? = null,
    val quantity: Double = 1.0,
    val unit: String = "шт",
    val price: Double? = null,
    @SerialName("is_checked") val isChecked: Boolean = false
)
@Serializable
data class Product(
    val id: String = "",
    @SerialName("name_ru") val nameRu: String = "",
    @SerialName("name_nl") val nameNl: String = "",
    @SerialName("default_store_id") val defaultStoreId: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)