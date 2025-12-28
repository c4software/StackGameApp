package com.vbrosseau.stackgame.di

import com.vbrosseau.stackgame.api.AuthService
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.data.UserRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection modules
 */

val appModule = module {
    // UserPreferences - Singleton
    single { UserPreferences(androidContext()) }
    
    // AuthService - Real API using cours.brosseau.ovh
    single<AuthService> { AuthService() }
    
    // UserRepository - Singleton
    single { UserRepository(get(), get()) }
}
