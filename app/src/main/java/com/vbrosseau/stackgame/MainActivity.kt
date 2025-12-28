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
import com.vbrosseau.stackgame.ui.screens.purchase.PurchaseScreen
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
                    onLoginClick = { viewModel.onLoginClick() },
                    onGuestClick = { viewModel.onPlayAsGuest() }
                )
            }
            
            Screen.Login -> {
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val error by viewModel.loginError.collectAsStateWithLifecycle()
                
                LoginScreen(
                    onLogin = { email -> viewModel.onLogin(email) },
                    onBack = { viewModel.onLoginBack() },
                    isLoading = isLoading,
                    error = error
                )
            }
            
            Screen.Profile -> {
                val user = uiState.currentUser ?: User("guest", "", UserLevel.NORMAL)
                
                ProfileScreen(
                    user = user,
                    onLogout = { viewModel.onLogout() },
                    onBack = { viewModel.onBackToGame() },
                    onPurchaseClick = { viewModel.onPurchaseClick() }
                )
            }
            
            Screen.Purchase -> {
                val user = uiState.currentUser ?: User("guest", "", UserLevel.NORMAL)
                
                PurchaseScreen(
                    currentLevel = user.level,
                    onBack = { viewModel.onProfileClick() }
                )
            }
            
            Screen.Game -> {
                val user = uiState.currentUser ?: User("guest", "", UserLevel.NORMAL)
                
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets(0, 0, 0, 0)), // Ignore system bars
                    bottomBar = {
                        if (user.showsAds()) {
                            AdBanner()
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0) // No content padding
                ) { innerPadding ->
                    StackGame(
                        user = user,
                        onLoginClick = { viewModel.onProfileClick() },
                        onPurchaseClick = { viewModel.onPurchaseClick() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
