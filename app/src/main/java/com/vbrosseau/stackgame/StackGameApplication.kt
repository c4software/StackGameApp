package com.vbrosseau.stackgame

import android.app.Application
import com.vbrosseau.stackgame.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class StackGameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Start Koin for dependency injection
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@StackGameApplication)
            modules(appModule)
        }
    }
}
