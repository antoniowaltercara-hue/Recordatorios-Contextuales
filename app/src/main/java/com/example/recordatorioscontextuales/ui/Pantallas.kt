package com.example.recordatorioscontextuales.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.recordatorioscontextuales.presentation.*
import com.example.recordatorioscontextuales.data.Escenario
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaListado(estado: ListadoUiState, onCrearRecordatorio: () -> Unit, onRecordatorioClick: (Int) -> Unit,
    onReintentar: () -> Unit, onFiltro: (Filtro) -> Unit, onSimular: (Escenario) -> Unit, modifier: Modifier = Modifier,
    onAjustes: () -> Unit = {}) {
    var simulacionVisible by rememberSaveable { mutableStateOf(false) }
    val lista = rememberLazyListState()
    val filtro = when (estado) {
        is ListadoUiState.Datos -> estado.filtro
        is ListadoUiState.Vacio -> estado.filtro
        else -> Filtro.Todos
    }
    LaunchedEffect(filtro) { lista.scrollToItem(0) }
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Mis recordatorios") },
        actions = { TextButton(onClick = onAjustes) { Text("Ajustes") } }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio, modifier = Modifier.semantics { contentDescription = "Crear recordatorio" }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            Text("Actividad 3 · Guardado local y sincronización", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { simulacionVisible = !simulacionVisible }) { Text(if (simulacionVisible) "Ocultar simulación" else "Simular estados") }
            if (simulacionVisible) {
                Text("Pruebas en memoria. Vacío no borra los datos.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Escenario.entries.forEach { escenario ->
                        OutlinedButton(onClick = { onSimular(escenario) }) { Text(when (escenario) {
                            Escenario.Datos -> "Datos"
                            Escenario.Carga -> "Carga"
                            Escenario.Vacio -> "Vacío"
                            Escenario.Error -> "Error"
                        }) }
                    }
                }
            }
            if (estado is ListadoUiState.Datos || estado is ListadoUiState.Vacio) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Filtro.entries.forEach { opcion ->
                        FilterChip(selected = filtro == opcion, onClick = { onFiltro(opcion) }, label = { Text(opcion.etiqueta) })
                    }
                }
            }
            when (estado) {
                ListadoUiState.Cargando -> MensajeEstado(TipoMensaje.Carga, "Cargando recordatorios…")
                is ListadoUiState.Error -> MensajeEstado(TipoMensaje.Error, estado.mensaje, accion = "Reintentar", onAccion = onReintentar)
                is ListadoUiState.Vacio -> MensajeEstado(TipoMensaje.Vacio, estado.mensaje, accion = "Crear el primero", onAccion = onCrearRecordatorio)
                is ListadoUiState.Datos -> {
                    Text("${estado.recordatorios.size} recordatorios · ${estado.categorias} categorías", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(state = lista, modifier = Modifier.weight(1f).testTag("lista"),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(estado.recordatorios, key = { it.id }) { registro ->
                            RecordatorioCard(registro, onSeleccionar = { onRecordatorioClick(registro.id) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCreacion(estado: CreacionUiState, onTitulo: (String) -> Unit, onDescripcion: (String) -> Unit,
    onCategoria: (com.example.recordatorioscontextuales.Categoria) -> Unit,
    onPrioridad: (com.example.recordatorioscontextuales.Prioridad) -> Unit,
    onGuardar: () -> Unit, onCancelar: () -> Unit, onSimularError: (Boolean) -> Unit, modifier: Modifier = Modifier,
    onEstado: (com.example.recordatorioscontextuales.EstadoRecordatorio) -> Unit = {}) {
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(if (estado.editando) "Editar recordatorio" else "Nuevo recordatorio") }, navigationIcon = { TextButton(onClick = onCancelar) { Text("Volver") } })
    }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoFormulario(estado.titulo, onTitulo, "Título", estado.errores.titulo,
                habilitado = !estado.enviando, modifier = Modifier.testTag("titulo"))
            CampoFormulario(estado.descripcion, onDescripcion, "Descripción", estado.errores.descripcion,
                habilitado = !estado.enviando, multilinea = true, ayuda = "${estado.descripcion.length}/200 caracteres",
                modifier = Modifier.testTag("descripcion"))
            SelectorOpciones("Categoría", categoriasPermitidas, estado.categoria, onCategoria,
                { it.etiqueta }, estado.errores.categoria, habilitado = !estado.enviando)
            SelectorOpciones("Prioridad", prioridadesPermitidas, estado.prioridad, onPrioridad,
                { it.etiqueta }, estado.errores.prioridad, habilitado = !estado.enviando)
            if (estado.editando) SelectorOpciones("Estado", listOf(
                com.example.recordatorioscontextuales.EstadoRecordatorio.Pendiente,
                com.example.recordatorioscontextuales.EstadoRecordatorio.EnCurso,
                com.example.recordatorioscontextuales.EstadoRecordatorio.Completado), estado.estado,
                onEstado, { it.etiqueta }, null, habilitado = !estado.enviando)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = estado.simularError, onCheckedChange = onSimularError,
                    enabled = !estado.enviando, modifier = Modifier.semantics { contentDescription = "Simular error de guardado" })
                Text("Simular error de guardado", style = MaterialTheme.typography.bodySmall)
            }
            when (val resultado = estado.resultado) {
                ResultadoGuardado.Enviando -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Guardando…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                is ResultadoGuardado.Error -> Text(resultado.mensaje, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive; error(resultado.mensaje) })
                is ResultadoGuardado.Exito -> Text("Recordatorio guardado")
                ResultadoGuardado.SinEnviar -> Unit
            }
            Button(onClick = onGuardar, enabled = estado.puedeGuardar,
                modifier = Modifier.fillMaxWidth().testTag("guardar")) { Text(if (estado.enviando) "Guardando…" else "Guardar") }
            OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            Text("Los datos quedan guardados en el dispositivo, incluso sin conexión.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalle(estado: DetalleUiState, onVolver: () -> Unit, onReintentar: () -> Unit, modifier: Modifier = Modifier,
    onEditar: () -> Unit = {}, onEliminar: () -> Unit = {}, mensaje: String? = null) {
    val formato = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    var confirmarEliminacion by remember { mutableStateOf(false) }
    if (confirmarEliminacion) AlertDialog(onDismissRequest = { confirmarEliminacion = false },
        title = { Text("Eliminar recordatorio") }, text = { Text("El registro se ocultará del listado y la eliminación se sincronizará cuando haya conexión.") },
        confirmButton = { TextButton(onClick = { confirmarEliminacion = false; onEliminar() }) { Text("Confirmar eliminación") } },
        dismissButton = { TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") } })
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text("Detalle del recordatorio") }, navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } })
    }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mensaje != null) Text(mensaje, color = MaterialTheme.colorScheme.error)
            when (estado) {
                DetalleUiState.Cargando -> MensajeEstado(TipoMensaje.Carga, "Cargando detalle…")
                is DetalleUiState.Error -> MensajeEstado(TipoMensaje.Error, estado.mensaje, accion = "Reintentar", onAccion = onReintentar)
                is DetalleUiState.Datos -> {
                    val r = estado.recordatorio
                    Text(r.titulo, style = MaterialTheme.typography.headlineSmall)
                    Text("Identificador: ${r.id}")
                    Text("Descripción: ${r.descripcion.ifBlank { "Sin descripción" }}")
                    Text("Categoría: ${r.categoria.etiqueta}")
                    Text("Prioridad: ${r.prioridad.etiqueta}")
                    Text("Estado: ${r.estado.etiqueta}")
                    HorizontalDivider()
                    Text("Vencimiento: ${r.fechaVencimiento?.format(formato) ?: "Sin fecha"}")
                    Text("Ubicación: ${r.ubicacion?.let { "${it.descripcion ?: "Sin nombre"} (${it.latitud}, ${it.longitud})" } ?: "Sin ubicación"}")
                    Text("Aviso: ${r.aviso?.format(formato) ?: "Sin aviso"}")
                    Text("Sincronizado: ${if (r.sincronizado) "Sí" else "No"}")
                    Text("Sincronización: ${r.estadoSincronizacion}")
                    Text("Creado: ${r.fechaCreacion.format(formato)}")
                    Text("Ubicación y avisos siguen siendo datos del modelo; todavía no se activan mapas ni notificaciones.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onEditar) { Text("Editar") }
                        OutlinedButton(onClick = { confirmarEliminacion = true }) { Text("Eliminar") }
                    }
                    Button(onClick = onVolver) { Text("Volver al listado") }
                }
            }
        }
    }
}
