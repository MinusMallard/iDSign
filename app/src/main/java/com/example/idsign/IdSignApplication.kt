package com.example.idsign

import android.app.Application
import com.example.idsign.data.AppContainer
import com.example.idsign.data.DefaultAppContainer

class IdSignApplication: Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}