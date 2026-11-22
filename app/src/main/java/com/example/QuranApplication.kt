package com.example

import android.app.Application
import com.example.audio.MurottalPlayerManager
import com.example.data.QuranRepository
import com.example.data.local.AppPreferences
import com.example.data.local.QuranDatabase
import com.example.data.remote.EquranApiService
import com.example.prayer.AzanNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuranApplication : Application() {

    lateinit var database: QuranDatabase
        private set
    lateinit var preferences: AppPreferences
        private set
    lateinit var repository: QuranRepository
        private set
    lateinit var audioManager: MurottalPlayerManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = QuranDatabase.getInstance(this)
        preferences = AppPreferences(this)
        repository = QuranRepository(
            context = this,
            dao = database.quranDao(),
            api = EquranApiService.create()
        )
        audioManager = MurottalPlayerManager(this)

        AzanNotificationHelper.ensureNotificationChannel(this)

        appScope.launch {
            repository.initializeDatabase()
            val settings = preferences.settingsFlow.first()
            AzanNotificationHelper.scheduleDailyAlarms(this@QuranApplication, settings)
        }
    }
}
