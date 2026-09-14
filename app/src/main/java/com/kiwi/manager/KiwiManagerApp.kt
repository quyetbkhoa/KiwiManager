package com.kiwi.manager

import android.app.Application

class KiwiManagerApp : Application() {
    companion object {
        lateinit var instance: KiwiManagerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
