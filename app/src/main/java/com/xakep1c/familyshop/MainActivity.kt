package com.xakep1c.familyshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xakep1c.familyshop.ui.ShoppingItemsScreen
import com.xakep1c.familyshop.ui.ShoppingListScreen
import com.xakep1c.familyshop.ui.theme.FamilyShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamilyShopTheme {
                var selectedList by remember { mutableStateOf<Pair<String, String>?>(null) }

                selectedList?.let { (listId, listName) ->
                    ShoppingItemsScreen(
                        listId = listId,
                        listName = listName,
                        onBack = { selectedList = null }
                    )
                } ?: run {
                    ShoppingListScreen(
                        onListClick = { id, name ->
                            selectedList = id to name
                        }
                    )
                }
            }
        }
    }
}
