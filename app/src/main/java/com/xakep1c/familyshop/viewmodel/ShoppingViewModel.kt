package com.xakep1c.familyshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xakep1c.familyshop.model.ShoppingList
import com.xakep1c.familyshop.model.ShoppingListItem
import com.xakep1c.familyshop.model.Store
import com.xakep1c.familyshop.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.xakep1c.familyshop.model.Product

class ShoppingViewModel : ViewModel() {

    private val repository = ShoppingRepository()

    // Состояния UI
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores

    private val _shoppingLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val shoppingLists: StateFlow<List<ShoppingList>> = _shoppingLists

    private val _items = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val items: StateFlow<List<ShoppingListItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    // Загрузка при старте
    init {
        loadStores()
        loadShoppingLists()
        loadProducts()
    }

    fun loadStores() {
        viewModelScope.launch {
            try {
                _stores.value = repository.getStores()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadShoppingLists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getShoppingLists()
                Log.d("ViewModel", "Lists loaded: ${result.size}")
                _shoppingLists.value = result
            } catch (e: Exception) {
                Log.e("ViewModel", "Lists error: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadItems(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getItems(listId)
                Log.d("ViewModel", "Items loaded: ${result.size}")
                _items.value = result
            } catch (e: Exception) {
                Log.e("ViewModel", "Items error: ${e.message}", e)
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
                loadShoppingLists() // обновляем список
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun checkItem(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            try {
                repository.checkItem(itemId, checked)
                // обновляем локально без запроса к серверу
                _items.value = _items.value.map {
                    if (it.id == itemId) it.copy(isChecked = checked) else it
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
                _items.value = _items.value.filter { it.id != itemId }
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
        price: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val item = ShoppingListItem(
                    listId = listId,
                    customName = name,
                    storeId = storeId,
                    quantity = quantity,
                    price = price
                )
                repository.addItem(item)
                loadItems(listId)
            } catch (e: Exception) {
                Log.e("ViewModel", "Add item error: ${e.message}", e)
                _error.value = e.message
            }
        }
    }


    fun loadProducts() {
        viewModelScope.launch {
            try {
                _products.value = repository.getProducts()
            } catch (e: Exception) {
                Log.e("ViewModel", "Products error: ${e.message}", e)
            }
        }
    }
}