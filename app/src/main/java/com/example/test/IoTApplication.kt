package com.example.test

import android.app.Application
import com.example.test.data.UserRepository
import com.example.test.util.NetworkSyncManager

class IoTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa el repositorio con la base de datos local SQLite (ConexionDbHelper)
        UserRepository.init(this)
        // Inicializa el monitor de conectividad y sincronización automática
        NetworkSyncManager.init(this)
    }
}
