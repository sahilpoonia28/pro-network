package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.NetworkRepository
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val db = AppDatabase.getDatabase(this)
    val repository = NetworkRepository(db)

    setContent {
      MyApplicationTheme {
        val viewModel: NetworkViewModel = viewModel(factory = NetworkViewModelFactory(repository))
        ProNetworkApp(viewModel)
      }
    }
  }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Network : Screen("network", "Network", Icons.Filled.People, Icons.Outlined.People)
    object Messages : Screen("messages", "Messages", Icons.Filled.Email, Icons.Outlined.Email)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun ProNetworkApp(viewModel: NetworkViewModel) {
    val navController = rememberNavController()
    
    val items = listOf(Screen.Home, Screen.Network, Screen.Messages, Screen.Profile)
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Do not show bottom bar on chat screen
            if (currentRoute?.startsWith("chat/") != true) {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Messages) {
                                    BadgedBox(badge = { Badge { Text("1") } }) {
                                        Icon(if (selected) screen.icon else screen.outlinedIcon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(if (selected) screen.icon else screen.outlinedIcon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                com.example.ui.HomeFeedScreen(viewModel)
            }
            composable(Screen.Network.route) {
                com.example.ui.NetworkScreen(viewModel)
            }
            composable(Screen.Messages.route) {
                com.example.ui.MessagesScreen(viewModel, onChatClick = { id, name, avatarUrl ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    val encodedAvatar = URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8.toString())
                    navController.navigate("chat/$id/$encodedName/$encodedAvatar")
                })
            }
            composable(
                route = "chat/{id}/{name}/{avatarUrl}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("avatarUrl") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
                val avatarUrl = URLDecoder.decode(backStackEntry.arguments?.getString("avatarUrl") ?: "", StandardCharsets.UTF_8.toString())
                
                com.example.ui.ChatScreen(
                    viewModel = viewModel,
                    counterpartId = id,
                    counterpartName = name,
                    counterpartAvatarUrl = avatarUrl,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                com.example.ui.ProfileScreen()
            }
        }
    }
}
