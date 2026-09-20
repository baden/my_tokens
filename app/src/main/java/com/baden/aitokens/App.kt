package com.baden.aitokens

import android.app.Application
import com.baden.aitokens.di.AppContainer

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
