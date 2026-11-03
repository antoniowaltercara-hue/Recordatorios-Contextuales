package com.example.recordatorioscontextuales.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.recordatorioscontextuales.*

@Composable
fun RecordatorioCard(recordatorio: Recordatorio, onSeleccionar: () -> Unit, modifier: Modifier = Modifier) {
    val estilo = ReminderData.estiloCategoria.getValue(recordatorio.categoria)
    Card(onClick = onSeleccionar, modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(recordatorio.titulo, style = MaterialTheme.typography.titleMedium)
            Text("Categoría: ${estilo.first}", color = estilo.second)
            Text("Prioridad: ${recordatorio.prioridad.etiqueta}")
            Text("Estado: ${recordatorio.estado.etiqueta}")
            Text("Ver detalle", style = MaterialTheme.typography.labelMedium)
        }
    }
}

enum class TipoMensaje { Carga, Vacio, Error }

@Composable
fun MensajeEstado(tipo: TipoMensaje, mensaje: String, modifier: Modifier = Modifier,
    accion: String? = null, onAccion: () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (tipo == TipoMensaje.Carga) CircularProgressIndicator()
        Text(mensaje, style = MaterialTheme.typography.bodyLarge)
        if (accion != null) Button(onClick = onAccion) { Text(accion) }
    }
}

@Composable
fun CampoFormulario(valor: String, onCambio: (String) -> Unit, etiqueta: String, mensajeError: String?,
    modifier: Modifier = Modifier, habilitado: Boolean = true, multilinea: Boolean = false, ayuda: String? = null) {
    OutlinedTextField(value = valor, onValueChange = onCambio, label = { Text(etiqueta) },
        enabled = habilitado, singleLine = !multilinea, minLines = if (multilinea) 3 else 1,
        isError = mensajeError != null,
        supportingText = { if (mensajeError != null || ayuda != null) Text(mensajeError ?: ayuda.orEmpty()) },
        keyboardOptions = KeyboardOptions(imeAction = if (multilinea) ImeAction.Default else ImeAction.Next),
        modifier = modifier.fillMaxWidth().semantics { if (mensajeError != null) error(mensajeError) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectorOpciones(etiqueta: String, opciones: List<T>, seleccion: T?, onSeleccionar: (T) -> Unit,
    texto: (T) -> String, mensajeError: String?, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Column(modifier.fillMaxWidth().semantics { if (mensajeError != null) error(mensajeError) },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                FilterChip(selected = opcion == seleccion, onClick = { onSeleccionar(opcion) },
                    enabled = habilitado, label = { Text(texto(opcion)) })
            }
        }
        if (mensajeError != null) Text(mensajeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
