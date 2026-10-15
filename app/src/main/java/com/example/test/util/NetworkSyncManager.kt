package com.example.test.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.test.data.UserRepository

object NetworkSyncManager {

    var isOnline: Boolean = true
        private set

    private var isSyncing = false
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isOnline = checkCurrentConnection(connectivityManager)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val wasOffline = !isOnline
                isOnline = true

                if (wasOffline) {
                    handler.post {
                        Toast.makeText(context, "Conexión a Internet recuperada. Sincronizando datos...", Toast.LENGTH_SHORT).show()
                        syncPendingChanges(context)
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isOnline = false
                handler.post {
                    Toast.makeText(context, "Modo sin conexión activado (SQLite local)", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun checkCurrentConnection(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun syncPendingChanges(context: Context, onFinished: ((Int, Boolean) -> Unit)? = null) {
        if (!isOnline || isSyncing) return

        val pendingUsers = UserRepository.getPendingSyncUsers()
        if (pendingUsers.isEmpty()) {
            onFinished?.invoke(0, true)
            return
        }

        isSyncing = true
        val queue = Volley.newRequestQueue(context)
        var syncedCount = 0

        // Procesa cada cambio pendiente hacia la API de MySQL
        for (user in pendingUsers) {
            val url = "http://52.2.255.205/sync_usuario.php" // Endpoint en AWS
            val request = object : StringRequest(
                Method.POST, url,
                { response ->
                    UserRepository.markSynced(user.id)
                    syncedCount++
                    if (syncedCount == pendingUsers.size) {
                        isSyncing = false
                        handler.post {
                            Toast.makeText(context, "Sincronización completada: $syncedCount cambios sincronizados con MySQL en AWS", Toast.LENGTH_LONG).show()
                            onFinished?.invoke(syncedCount, true)
                        }
                    }
                },
                { error ->
                    // Si falla el servidor, se mantiene local en SQLite para reintentar (Requerimiento Pauta N° 13)
                    isSyncing = false
                    onFinished?.invoke(syncedCount, false)
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return mutableMapOf(
                        "id" to user.id.toString(),
                        "nombres" to user.nombres,
                        "apellidos" to user.apellidos,
                        "email" to user.email,
                        "clave" to user.clave,
                        "sync_action" to user.syncStatus
                    )
                }
            }
            queue.add(request)
        }
    }
}
