package com.oryareach.app

import android.app.Application
import com.oryareach.app.di.appModule
import com.oryareach.core.network.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaharApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@SaharApplication)
            modules(appModule, networkModule)
        }
    }
}
