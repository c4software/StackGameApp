package com.vbrosseau.stackgame.di

import com.vbrosseau.stackgame.api.AuthService
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.data.UserRepository
import com.vbrosseau.stackgame.ui.MainViewModel
import com.vbrosseau.stackgame.ui.screens.login.LoginViewModel
import com.vbrosseau.stackgame.ui.screens.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Data layer
    single { UserPreferences(androidContext()) }
    single { AuthService() }
    single { UserRepository(get(), get()) }
    
    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { MainViewModel(get()) }
}
