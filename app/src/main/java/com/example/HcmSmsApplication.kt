package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration

class HcmSmsApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "HcmSmsApp"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "HcmSmsApplication initialized")
    }
}
