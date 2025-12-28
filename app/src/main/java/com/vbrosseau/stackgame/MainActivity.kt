package com.vbrosseau.stackgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import com.vbrosseau.stackgame.ui.MainViewModel
import com.vbrosseau.stackgame.ui.Screen
import com.vbrosseau.stackgame.ui.screens.game.StackGame
import com.vbrosseau.stackgame.ui.screens.login.LoginScreen
import com.vbrosseau.stackgame.ui.screens.onboarding.OnboardingScreen
import com.vbrosseau.stackgame.ui.screens.profile.ProfileScreen
import com.vbrosseau.stackgame.ui.components.AdBanner
import com.vbrosseau.stackgame.ui.theme.StackGameTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            StackGameApp(mainViewModel)
        }
    }
}

@Composable
fun StackGameApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    StackGameTheme {
        when (uiState.currentScreen) {
            Screen.Loading -> {
                // Show nothing while loading
            }
            Screen.Onboarding -> {
                OnboardingScreen(
                    onComplete = { viewModel.onOnboardingComplete() }
                )
            }
            Screen.Login -> {
                LoginScreen(
                    onLoginSuccess = { user -> viewModel.onLoginSuccess(user) },
                    onContinueAsGuest = { viewModel.onContinueAsGuest() }
                )
            }
            Screen.Profile -> {
                val user = uiState.currentUser ?: User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                
                ProfileScreen(
                    user = user,
                    onLogout = { viewModel.onLogout() },
                    onBack = { viewModel.onBackToGame() }
                )
            }
            Screen.Game -> {
                val user = uiState.currentUser ?: User("Joueur", "", UserLevel.NORMAL, isGuest = true)
                
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
                        onLoginClick = { viewModel.onProfileClick() },
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
