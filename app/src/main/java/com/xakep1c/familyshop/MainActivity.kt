package com.xakep1c.familyshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xakep1c.familyshop.ui.LoginScreen
import com.xakep1c.familyshop.ui.ShoppingItemsScreen
import com.xakep1c.familyshop.ui.ShoppingListScreen
import com.xakep1c.familyshop.ui.theme.FamilyShopTheme
import com.xakep1c.familyshop.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamilyShopTheme {
                FamilyShopApp()
            }
        }
    }
}

@Composable
fun FamilyShopApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    // ВРЕМЕННО для теста: заходим без логина
    val isLoggedIn = true // by authViewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(authViewModel)
    } else {
        NavHost(
            navController = navController,
            startDestination = "shopping_lists"
        ) {
            composable("shopping_lists") {
                ShoppingListScreen(
                    onListClick = { id, name ->
                        navController.navigate("shopping_items/$id/$name")
                    }
                )
            }
            composable(
                route = "shopping_items/{listId}/{listName}",
                arguments = listOf(
                    navArgument("listId") { type = NavType.StringType },
                    navArgument("listName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: ""
                val listName = backStackEntry.arguments?.getString("listName") ?: ""
                ShoppingItemsScreen(
                    listId = listId,
                    listName = listName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
