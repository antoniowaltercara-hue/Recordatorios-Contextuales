package com.example.recordatorioscontextuales.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorioscontextuales.data.RepositorioPersistente
import com.example.recordatorioscontextuales.data.local.*
import kotlinx.coroutines.launch

class AjustesViewModel(private val repo: RepositorioPersistente) : ViewModel() {
    val preferencias = repo.preferencias
    val sincronizacion = repo.sincronizacion
    val sinRed = repo.sinRedSimulada
    fun guardar(p: Preferencias) { viewModelScope.launch { repo.guardarPreferencias(p) } }
    fun sincronizar() { viewModelScope.launch { repo.sincronizar() } }
    fun simularSinRed(v: Boolean) = repo.simularSinRed(v)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(vm: AjustesViewModel, onVolver: () -> Unit) {
    val p by vm.preferencias.collectAsStateWithLifecycle()
    val sync by vm.sincronizacion.collectAsStateWithLifecycle()
    val sinRed by vm.sinRed.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") },
        navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Preferencias y sincronización", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tema oscuro", Modifier.weight(1f))
                Switch(checked = p.oscuro, onCheckedChange = { vm.guardar(p.copy(oscuro = it)) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mostrar completados", Modifier.weight(1f))
                Switch(checked = p.mostrarCompletados, onCheckedChange = { vm.guardar(p.copy(mostrarCompletados = it)) })
            }
            SelectorOpciones("Orden del listado", Orden.entries.toList(), p.orden,
                { vm.guardar(p.copy(orden = it)) }, { it.name }, null)
            Text("Estas tres preferencias se guardan con DataStore. Los recordatorios se guardan con Room.")
            HorizontalDivider()
            Text("Conexión y pendientes", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Simular sin conexión", Modifier.weight(1f))
                Switch(checked = sinRed, onCheckedChange = vm::simularSinRed)
            }
            Text(sync.mensaje, color = if (sync.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (sync.ocupada) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(onClick = vm::sincronizar, enabled = !sync.ocupada) { Text("Sincronizar ahora") }
            Text("La app conserva los datos aunque el servidor no esté iniciado. Al recuperar la red o volver a abrir la app se intenta sincronizar.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
