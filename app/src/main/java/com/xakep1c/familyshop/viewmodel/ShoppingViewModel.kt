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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository

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

    fun searchOnline(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _onlineSearchResults.value = emptyList()
            try {
                val response = supabase.functions.invoke("search-products", 
                    body = buildJsonObject { put("query", query) }
                )
                val results = response.decodeAs<List<OnlineProduct>>()
                _onlineSearchResults.value = results
            } catch (e: Exception) {
                Log.e("ViewModel", "Online search error", e)
                _error.value = "Ошибка поиска онлайн: ${e.localizedMessage}"
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
                val oldItems = dao.getItemsByListIdDirect(fromListId)
                
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
}
