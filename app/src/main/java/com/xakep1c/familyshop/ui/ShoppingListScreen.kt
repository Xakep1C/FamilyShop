package com.xakep1c.familyshop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xakep1c.familyshop.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onListClick: (String, String) -> Unit,
    viewModel: ShoppingViewModel = viewModel()
) {
    val shoppingLists by viewModel.shoppingLists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("FamilyShop 🛒") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Новый список")
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shoppingLists) { list ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onListClick(list.id, list.name) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (list.isCompleted) "✅ Завершён" else "🛒 Активный",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Новый список") },
                text = {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Название") },
                        placeholder = { Text("Например: Суббота, Albert Heijn") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newListName.isNotBlank()) {
                            viewModel.createShoppingList(newListName)
                            newListName = ""
                            showDialog = false
                        }
                    }) { Text("Создать") }
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