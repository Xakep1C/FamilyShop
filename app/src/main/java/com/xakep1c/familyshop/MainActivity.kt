package com.xakep1c.familyshop

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.xakep1c.familyshop.ui.ShoppingItemsScreen
import com.xakep1c.familyshop.ui.theme.FamilyShopTheme
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.xakep1c.familyshop.ui.ShoppingListScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Временный тест shopping_lists
        lifecycleScope.launch {
            try {
                val result = supabase.postgrest["shopping_lists"].select()
                Log.d("Supabase", "Lists: ${result.data}")
            } catch (e: Exception) {
                Log.e("Supabase", "Lists Error: ${e.message}")
            }
        }

        setContent {
            FamilyShopTheme {
                var selectedList by remember { mutableStateOf<Pair<String, String>?>(null) }

                if (selectedList == null) {
                    ShoppingListScreen(
                        onListClick = { id, name ->
                            selectedList = Pair(id, name)
                        }
                    )
                } else {
                    ShoppingItemsScreen(
                        listId = selectedList!!.first,
                        listName = selectedList!!.second,
                        onBack = { selectedList = null }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FamilyShopTheme {
        Greeting("Android")
    }
}