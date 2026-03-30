package com.xakep1c.familyshop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xakep1c.familyshop.model.ShoppingList
import com.xakep1c.familyshop.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onListClick: (String, String) -> Unit,
    viewModel: ShoppingViewModel = viewModel()
) {
    val activeLists by viewModel.activeLists.collectAsState()
    val completedLists by viewModel.completedLists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf<ShoppingList?>(null) }
    var newListName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FamilyShop 🛒") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Новый список")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Активные (${activeLists.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("История (${completedLists.size})") }
                )
            }

            if (isLoading && activeLists.isEmpty() && completedLists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentLists = if (selectedTab == 0) activeLists else completedLists
                
                if (currentLists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTab == 0) "Нет активных списков" else "История пуста",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentLists) { list ->
                            ShoppingListCard(
                                list = list,
                                isHistory = selectedTab == 1,
                                onClick = { onListClick(list.id, list.name) },
                                onComplete = { viewModel.completeList(list.id) },
                                onRepeat = { showRepeatDialog = list }
                            )
                        }
                    }
                }
            }
        }

        // Диалог создания нового списка
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Новый список покупок") },
                text = {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Название") },
                        placeholder = { Text("Например: Продукты на неделю") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newListName.isNotBlank()) {
                            viewModel.createShoppingList(newListName)
                            newListName = ""
                            showAddDialog = false
                        }
                    }) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
                }
            )
        }

        // Диалог выбора списка для повтора заказа
        showRepeatDialog?.let { oldList ->
            AlertDialog(
                onDismissRequest = { showRepeatDialog = null },
                title = { Text("Повторить заказ?") },
                text = { Text("Все товары из списка '${oldList.name}' будут скопированы в выбранный активный список.") },
                confirmButton = {
                    Column {
                        activeLists.forEach { active ->
                            Button(
                                onClick = {
                                    viewModel.copyItemsFromList(oldList.id, active.id)
                                    showRepeatDialog = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("В список: ${active.name}")
                            }
                        }
                        if (activeLists.isEmpty()) {
                            Text("Сначала создайте активный список", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRepeatDialog = null }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun ShoppingListCard(
    list: ShoppingList,
    isHistory: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onRepeat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Создан: ${list.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (!isHistory) {
                IconButton(onClick = onComplete) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = "Завершить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(onClick = onRepeat) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Повторить заказ",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
