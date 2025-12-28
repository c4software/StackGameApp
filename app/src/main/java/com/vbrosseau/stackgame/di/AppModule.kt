package com.vbrosseau.stackgame.di

import com.vbrosseau.stackgame.data.BillingManager
import com.vbrosseau.stackgame.data.PlayGamesManager
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.ui.MainViewModel
import com.vbrosseau.stackgame.ui.screens.profile.ProfileViewModel
import com.vbrosseau.stackgame.ui.screens.purchase.PurchaseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Data layer
    single { UserPreferences(androidContext()) }
    single { BillingManager(androidContext()) }
    // PlayGamesManager needs Activity, will be created in MainActivity
    
    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { PurchaseViewModel(get()) }
}
