package com.xakep1c.familyshop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xakep1c.familyshop.model.OnlineProduct
import com.xakep1c.familyshop.viewmodel.ShoppingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemsScreen(
    listId: String,
    listName: String,
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listId) {
        viewModel.loadItems(listId)
    }

    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->

        if (isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    ItemCard(
                        name = item.customName,
                        price = item.price,
                        imageUrl = item.imageUrl,
                        isChecked = item.isChecked,
                        onCheckedChange = { viewModel.checkItem(item.id, it) },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
            }
        }

        if (showDialog) {
            AddItemDialog(
                onDismiss = { 
                    viewModel.clearOnlineResults()
                    showDialog = false 
                },
                onConfirm = { name, storeId, qty, price, img ->
                    viewModel.addItemByName(listId, name, storeId, qty, price, img)
                    showDialog = false
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ItemCard(
    name: String,
    price: Double?,
    imageUrl: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
            
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(horizontal = 4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isChecked) Color.Gray else Color.Unspecified
                    )
                )
                if (price != null) {
                    Text("${price} €", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Удалить", tint = Color.Red.copy(0.6f))
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double, Double?, String?) -> Unit,
    viewModel: ShoppingViewModel
) {
    val onlineResults by viewModel.onlineSearchResults.collectAsState()
    val stores by viewModel.stores.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedStoreId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить товар") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Что ищем?") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.searchOnline(name) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, "Поиск онлайн", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (onlineResults.isNotEmpty()) {
                    Text("Найдено в магазинах 🇳🇱:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    ) {
                        items(onlineResults) { res ->
                            OnlineProductCard(res) {
                                name = res.name
                                price = res.price?.toString() ?: ""
                                selectedImageUrl = res.imageUrl
                                // Пытаемся сопоставить магазин
                                selectedStoreId = stores.find { it.name.contains(res.storeName, true) }?.id
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Кол-во") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена €") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name, selectedStoreId, quantity.toDoubleOrNull() ?: 1.0, price.toDoubleOrNull(), selectedImageUrl)
                }
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun OnlineProductCard(product: OnlineProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(120.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = product.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text("${product.price} €", style = MaterialTheme.typography.labelSmall)
            Text(
                text = product.storeName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}
