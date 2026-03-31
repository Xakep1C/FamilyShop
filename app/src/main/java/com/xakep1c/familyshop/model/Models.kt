package com.xakep1c.familyshop.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnlineProduct(
    val name: String,
    val price: Double? = null,
    val storeName: String,
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val unit: String = "шт"
)

@Serializable
@Entity(tableName = "stores")
data class Store(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val color: String = "",
    val url: String? = null
)

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = "",
    @SerialName("name_ru") @ColumnInfo(name = "name_ru") val nameRu: String = "",
    @SerialName("name_nl") @ColumnInfo(name = "name_nl") val nameNl: String = ""
)

@Serializable
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey val id: String = "",
    val name: String = "Список",
    @SerialName("created_by") @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @SerialName("is_completed") @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: String = "",
    @SerialName("planned_date") @ColumnInfo(name = "planned_date") val plannedDate: String? = null
)

@Serializable
@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey val id: String = "",
    @SerialName("list_id") @ColumnInfo(name = "list_id") val listId: String = "",
    @SerialName("custom_name") @ColumnInfo(name = "custom_name") val customName: String = "",
    @SerialName("store_id") @ColumnInfo(name = "store_id") val storeId: String? = null,
    val quantity: Double = 1.0,
    val unit: String = "шт",
    val price: Double? = null,
    @SerialName("is_checked") @ColumnInfo(name = "is_checked") val isChecked: Boolean = false,
    @SerialName("image_url") @ColumnInfo(name = "image_url") val imageUrl: String? = null
)

@Serializable
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = "",
    @SerialName("name_ru") @ColumnInfo(name = "name_ru") val nameRu: String = "",
    @SerialName("name_nl") @ColumnInfo(name = "name_nl") val nameNl: String = "",
    @SerialName("category_id") @ColumnInfo(name = "category_id") val categoryId: String? = null,
    val classifier: String? = null,
    val barcode: String? = null,
    @SerialName("photo_url") @ColumnInfo(name = "photo_url") val photoUrl: String? = null,
    @SerialName("photo_fetched_at") @ColumnInfo(name = "photo_fetched_at") val photoFetchedAt: String? = null,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: String? = null,
    val url: String? = null
)
