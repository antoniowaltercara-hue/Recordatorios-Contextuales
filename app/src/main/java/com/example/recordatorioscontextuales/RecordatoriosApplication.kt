package com.example.recordatorioscontextuales

import android.app.Application
import android.net.*
import com.example.recordatorioscontextuales.data.*
import com.example.recordatorioscontextuales.data.local.*
import com.example.recordatorioscontextuales.data.remote.ServicioFactory
import kotlinx.coroutines.*

class RecordatoriosApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val conectividad by lazy { getSystemService(ConnectivityManager::class.java) }
    val repository: RepositorioPersistente by lazy {
        LocalFirstRepository(BaseDatos.obtener(this), PreferenciasRepository(preferenciasDataStore),
            ServicioFactory.crear(), scope) { conectividad.activeNetwork != null }
    }
    override fun onCreate() {
        super.onCreate()
        scope.launch { repository.sincronizar() }
        conectividad.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { scope.launch { repository.sincronizar() } }
        })
    }
}
