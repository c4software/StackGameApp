package com.vbrosseau.stackgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import com.vbrosseau.stackgame.ui.*
import com.vbrosseau.stackgame.ui.theme.StackGameTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    // Inject UserPreferences using Koin
    private val userPreferences: UserPreferences by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            StackGameApp(userPreferences)
        }
    }
}

@Composable
fun StackGameApp(userPreferences: UserPreferences) {
    StackGameTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
        var currentUser by remember { mutableStateOf<User?>(null) }
        
        // Determine initial screen
        LaunchedEffect(Unit) {
            val savedUser = userPreferences.getUser()
            if (savedUser != null) {
                currentUser = savedUser
                // Check if we need to show onboarding (only on first launch)
                if (!userPreferences.hasCompletedOnboarding()) {
                    currentScreen = Screen.Onboarding
                } else {
                    currentScreen = Screen.Game
                }
            } else {
                // No saved user - show onboarding
                currentScreen = Screen.Onboarding
            }
        }
        
        when (currentScreen) {
            Screen.Loading -> {
                // Show nothing while loading
            }
            Screen.Onboarding -> {
                OnboardingScreen(
                    onComplete = {
                        userPreferences.setOnboardingCompleted(true)
                        // Create guest user
                        val guestUser = User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                        currentUser = guestUser
                        currentScreen = Screen.Game
                    }
                )
            }
            Screen.Login -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        userPreferences.saveUser(user)
                        currentScreen = Screen.Game
                    },
                    onContinueAsGuest = {
                        // Create guest user
                        val guestUser = User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                        currentUser = guestUser
                        currentScreen = Screen.Game
                    }
                )
            }
            Screen.Profile -> {
                val user = currentUser ?: User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                
                // If guest, redirect to login
                if (user.isGuest) {
                    LaunchedEffect(Unit) {
                        currentScreen = Screen.Login
                    }
                } else {
                    ProfileScreen(
                        user = user,
                        onLogout = {
                            userPreferences.logout()
                            currentUser = null
                            // Reset onboarding flag to show it again
                            userPreferences.setOnboardingCompleted(false)
                            currentScreen = Screen.Onboarding
                        },
                        onBack = {
                            currentScreen = Screen.Game
                        }
                    )
                }
            }
            Screen.Game -> {
                val user = currentUser ?: User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Show ad banner for NORMAL users
                        if (user.showsAds()) {
                            AdBanner()
                        }
                    }
                ) { innerPadding ->
                    StackGame(
                        user = user,
                        onLoginClick = {
                            currentScreen = Screen.Profile
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class Screen {
    Loading,
    Onboarding,
    Login,
    Profile,
    Game
}
