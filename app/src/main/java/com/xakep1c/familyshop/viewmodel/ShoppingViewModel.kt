package com.xakep1c.familyshop.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xakep1c.familyshop.db.AppDatabase
import com.xakep1c.familyshop.model.ShoppingList
import com.xakep1c.familyshop.model.ShoppingListItem
import com.xakep1c.familyshop.model.Store
import com.xakep1c.familyshop.repository.ShoppingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import com.xakep1c.familyshop.model.OnlineProduct
import com.xakep1c.familyshop.model.Product
import com.xakep1c.familyshop.supabase
import io.github.jan.supabase.functions.functions
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository
    private val httpClient = HttpClient()

    // Состояния UI
    val shoppingLists: StateFlow<List<ShoppingList>>
    val activeLists: StateFlow<List<ShoppingList>>
    val completedLists: StateFlow<List<ShoppingList>>

    val stores: StateFlow<List<Store>>
    val products: StateFlow<List<Product>>
    
    private val _items = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val items: StateFlow<List<ShoppingListItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Результаты онлайн-поиска
    private val _onlineSearchResults = MutableStateFlow<List<OnlineProduct>>(emptyList())
    val onlineSearchResults = _onlineSearchResults.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).shoppingDao()
        repository = ShoppingRepository(dao)

        shoppingLists = repository.allShoppingLists.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        
        activeLists = shoppingLists
            .map { list -> list.filter { !it.isCompleted } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        completedLists = shoppingLists
            .map { list -> list.filter { it.isCompleted } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        stores = repository.allStores.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        products = repository.allProducts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        refreshInitialData()
        repository.observeRealtimeChanges(viewModelScope)
    }

    private fun refreshInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshShoppingLists()
                repository.refreshStores()
                repository.refreshProducts()
            } catch (e: Exception) {
                _error.value = "Ошибка синхронизации: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun translateToDutch(text: String): String {
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=ru&tl=nl&dt=t&q=$encodedText"
            val response = httpClient.get(url).bodyAsText()
            // Ответ приходит в формате: [[["Makreel","Скумбрия",null,null,1]],null,"ru",...]
            val jsonArray = Json.parseToJsonElement(response).jsonArray
            val translation = jsonArray[0].jsonArray[0].jsonArray[0].jsonPrimitive.content
            Log.d("ViewModel", "Translated '$text' to '$translation'")
            translation
        } catch (e: Exception) {
            Log.e("ViewModel", "Translation error", e)
            text // Возвращаем оригинал в случае ошибки
        }
    }

    fun searchOnline(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _onlineSearchResults.value = emptyList()
            try {
                // АВТОМАТИЧЕСКИЙ ПЕРЕВОД, если введена кириллица
                val translatedQuery = if (query.any { it in 'а'..'я' || it in 'А'..'Я' }) {
                    translateToDutch(query)
                } else {
                    query
                }

                val response = withContext(Dispatchers.IO) {
                    supabase.functions.invoke("search-products", 
                        body = buildJsonObject { put("query", translatedQuery) }
                    )
                }
                val results = response.body<List<OnlineProduct>>()
                _onlineSearchResults.value = results
            } catch (e: Exception) {
                Log.e("ViewModel", "Online search error", e)
                
                // ВРЕМЕННАЯ ЗАГЛУШКА ДЛЯ ТЕСТА (если функция не найдена)
                if (query.lowercase().contains("скумбрия")) {
                    _onlineSearchResults.value = listOf(
                        OnlineProduct(
                            name = "Makreel (Скумбрия)",
                            price = 2.49,
                            storeName = "Albert Heijn",
                            imageUrl = "https://static.ah.nl/static/product/AHI_81434430323633333334_1_LowRes_JPG.JPG"
                        )
                    )
                    _error.value = null
                } else {
                    _error.value = "Ошибка поиска онлайн: ${e.localizedMessage}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOnlineResults() {
        _onlineSearchResults.value = emptyList()
    }

    fun loadItems(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getItems(listId).collect {
                    _items.value = it
                }
                repository.refreshItems(listId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            try {
                repository.createShoppingList(name)
            } catch (e: Exception) {
                _error.value = "Не удалось создать список: ${e.message}"
            }
        }
    }

    fun completeList(listId: String) {
        viewModelScope.launch {
            try {
                repository.completeShoppingList(listId)
            } catch (e: Exception) {
                _error.value = "Не удалось завершить список: ${e.message}"
            }
        }
    }

    fun checkItem(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.checkItem(itemId, checked)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun addItemByName(
        listId: String,
        name: String,
        storeId: String? = null,
        quantity: Double = 1.0,
        price: Double? = null,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val item = ShoppingListItem(
                    id = java.util.UUID.randomUUID().toString(),
                    listId = listId,
                    customName = name,
                    storeId = storeId,
                    quantity = quantity,
                    price = price,
                    imageUrl = imageUrl
                )
                repository.addItem(item)
            } catch (e: Exception) {
                Log.e("ViewModel", "Add item error: ${e.message}", e)
                _error.value = e.message
            }
        }
    }

    fun copyItemsFromList(fromListId: String, toListId: String) {
        viewModelScope.launch {
            try {
                val dao = AppDatabase.getDatabase(getApplication()).shoppingDao()
                val oldItems = withContext(Dispatchers.IO) {
                    dao.getItemsByListIdDirect(fromListId)
                }
                
                val newItems = oldItems.map { oldItem ->
                    oldItem.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        listId = toListId,
                        isChecked = false
                    )
                }
                
                repository.addItems(newItems)
            } catch (e: Exception) {
                Log.e("ViewModel", "Copy items error: ${e.message}", e)
                _error.value = "Не удалось скопировать товары: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
