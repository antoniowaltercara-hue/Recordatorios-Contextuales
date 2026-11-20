package com.example.recordatorioscontextuales.data.local

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

val Context.preferenciasDataStore by preferencesDataStore(name = "preferencias",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

enum class Orden { Fecha, Titulo, Prioridad }
data class Preferencias(val oscuro: Boolean = false, val mostrarCompletados: Boolean = true, val orden: Orden = Orden.Fecha)

class PreferenciasRepository(private val store: androidx.datastore.core.DataStore<Preferences>) {
    companion object {
        val OSCURO = booleanPreferencesKey("tema_oscuro")
        val COMPLETADOS = booleanPreferencesKey("mostrar_completados")
        val ORDEN = stringPreferencesKey("orden")
        fun interpretar(p: Preferences) = Preferencias(
            p.asMap()[OSCURO] as? Boolean ?: false,
            p.asMap()[COMPLETADOS] as? Boolean ?: true,
            Orden.entries.find { it.name == (p.asMap()[ORDEN] as? String) } ?: Orden.Fecha)
    }
    val flujo: Flow<Preferencias> = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::interpretar)
    suspend fun guardar(p: Preferencias) {
        store.edit { it[OSCURO] = p.oscuro; it[COMPLETADOS] = p.mostrarCompletados; it[ORDEN] = p.orden.name }
    }
}
