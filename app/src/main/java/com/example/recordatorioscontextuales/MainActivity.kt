package com.example.recordatorioscontextuales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF285C55),
                background = Color(0xFFF5F7F6),
                surface = Color.White
            )) { PantallaRecordatorios() }
        }
    }
}
@Composable
fun PantallaRecordatorios(
    estado: EstadoAplicacion = EstadoAplicacion.Lista
) {
    var filtro by remember { mutableStateOf("Todos") }
    val lista = rememberLazyListState()
    LaunchedEffect(filtro) { lista.scrollToItem(0) }
    val visibles = when (filtro) {
        "Pendientes" -> ReminderData.porEstado(EstadoRecordatorio.Pendiente)
        "Alta prioridad" -> ReminderData.porPrioridad(Prioridad.Alta)
        else -> ReminderData.recordatorios
    }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Mis recordatorios", style = MaterialTheme.typography.headlineSmall)
            Text("Actividad 1 · Programación Mobile",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            when (estado) {
                EstadoAplicacion.Cargando -> CircularProgressIndicator()
                is EstadoAplicacion.Error -> Text(estado.mensaje)
                EstadoAplicacion.Lista -> {
                    Text("${visibles.size} recordatorios · " +
                        "${ReminderData.categorias.size} categorías")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Todos", "Pendientes", "Alta prioridad").forEach { opcion ->
                            FilterChip(selected = filtro == opcion,
                                onClick = { filtro = opcion }, label = { Text(opcion) })
                        }
                    }
                    LazyColumn(
                        state = lista,
                        modifier = Modifier.weight(1f).testTag("lista"),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibles, key = { it.id }) {
                            RecordatorioCard(it)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun RecordatorioCard(recordatorio: Recordatorio) {
    val estilo = ReminderData.estiloCategoria.getValue(recordatorio.categoria)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(recordatorio.titulo, style = MaterialTheme.typography.titleMedium)
            Text(recordatorio.descripcion)
            Text("Categoría: ${estilo.first}", color = estilo.second)
            Text("Prioridad: ${recordatorio.prioridad.etiqueta}")
            Text("Estado: ${recordatorio.estado.etiqueta}")
        }
    }
}
