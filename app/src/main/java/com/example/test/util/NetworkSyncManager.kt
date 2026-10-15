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
import com.example.test.data.RemoteApiManager
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
        var errorCount  = 0
        val total = pendingUsers.size

        fun checkDone() {
            if (syncedCount + errorCount == total) {
                isSyncing = false
                handler.post {
                    if (syncedCount > 0) {
                        Toast.makeText(
                            context,
                            "Sincronización completada: $syncedCount cambio(s) sincronizado(s) con MySQL en AWS",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onFinished?.invoke(syncedCount, errorCount == 0)
                }
            }
        }

        for (user in pendingUsers) {
            val request = object : StringRequest(
                Method.POST,
                RemoteApiManager.API_SYNC,
                { _ ->
                    UserRepository.markSynced(user.id, user.email)
                    syncedCount++
                    checkDone()
                },
                { _ ->
                    errorCount++
                    checkDone()
                }
            ) {
                override fun getParams(): MutableMap<String, String> = mutableMapOf(
                    "id"          to user.id.toString(),
                    "nombres"     to user.nombres,
                    "apellidos"   to user.apellidos,
                    "email"       to user.email,
                    "clave"       to user.clave,
                    "sync_action" to user.syncStatus
                )
            }
            queue.add(request)
        }
    }
}
