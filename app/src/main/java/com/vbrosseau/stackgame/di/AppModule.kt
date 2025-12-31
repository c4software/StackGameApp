package com.vbrosseau.stackgame.di

import com.vbrosseau.stackgame.api.ApiClient
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.ui.MainViewModel
import com.vbrosseau.stackgame.ui.screens.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Data layer
    single { UserPreferences(androidContext()) }
    single { ApiClient() }
    
    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { com.vbrosseau.stackgame.ui.screens.game.GameViewModel() }
}
