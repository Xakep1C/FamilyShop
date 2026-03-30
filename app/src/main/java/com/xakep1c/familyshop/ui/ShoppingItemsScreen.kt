package com.xakep1c.familyshop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xakep1c.familyshop.viewmodel.ShoppingViewModel

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
    var showDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    LaunchedEffect(listId) {
        viewModel.loadItems(listId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить товар")
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список пуст\nНажми + чтобы добавить товар",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        viewModel.checkItem(item.id, checked)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.customName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (item.isChecked)
                                            TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.isChecked) Color.Gray else Color.Unspecified
                                    )
                                )
                            }
                            IconButton(onClick = { viewModel.deleteItem(item.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            val stores by viewModel.stores.collectAsState()
            var newItemName by remember { mutableStateOf("") }
            var selectedStoreId by remember { mutableStateOf<String?>(null) }
            var quantity by remember { mutableStateOf("1") }
            var price by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Новый товар") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Название") },
                            placeholder = { Text("Например: Молоко") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Выбор магазина
                        Text("Магазин:", style = MaterialTheme.typography.bodyMedium)
                        stores.forEach { store ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedStoreId == store.id,
                                    onClick = { selectedStoreId = store.id }
                                )
                                Text(store.name)
                            }
                        }
                        // Количество
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Количество") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Цена
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Цена (€)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newItemName.isNotBlank()) {
                            viewModel.addItemByName(
                                listId = listId,
                                name = newItemName,
                                storeId = selectedStoreId,
                                quantity = quantity.toDoubleOrNull() ?: 1.0,
                                price = price.toDoubleOrNull()
                            )
                            showDialog = false
                        }
                    }) { Text("Добавить") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}