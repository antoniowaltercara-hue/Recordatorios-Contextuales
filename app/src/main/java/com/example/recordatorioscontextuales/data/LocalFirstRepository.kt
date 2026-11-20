package com.example.recordatorioscontextuales.data

import androidx.room.withTransaction
import com.example.recordatorioscontextuales.*
import com.example.recordatorioscontextuales.data.local.*
import com.example.recordatorioscontextuales.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.UUID

data class Sincronizacion(val ocupada: Boolean = false, val mensaje: String = "Cambios guardados en el dispositivo", val error: Boolean = false)

interface RepositorioPersistente : RecordatoriosRepository {
    val preferencias: StateFlow<Preferencias>
    val sincronizacion: StateFlow<Sincronizacion>
    val sinRedSimulada: StateFlow<Boolean>
    suspend fun guardarPreferencias(p: Preferencias)
    suspend fun sincronizar()
    fun simularSinRed(valor: Boolean)
}

class LocalFirstRepository(
    private val db: BaseDatos,
    private val ajustes: PreferenciasRepository,
    private val servicio: ServicioRecordatorios,
    private val scope: CoroutineScope,
    private val hayRed: () -> Boolean = { true }
) : RepositorioPersistente {
    private val dao = db.recordatorios()
    private val bloqueoSync = Mutex()
    private val estadoSync = MutableStateFlow(Sincronizacion())
    override val sincronizacion = estadoSync.asStateFlow()
    private val sinRed = MutableStateFlow(false)
    override val sinRedSimulada = sinRed.asStateFlow()
    override val preferencias = ajustes.flujo.stateIn(scope, SharingStarted.Eagerly, Preferencias())
    override val cambios: StateFlow<List<Recordatorio>> = combine(dao.observar(), ajustes.flujo) { filas, p ->
        aplicarPreferencias(filas.map { it.aDominio() }, p)
    }.catch { fallo ->
        if (fallo is CancellationException) throw fallo
        estadoSync.value = Sincronizacion(mensaje = "No se pudo leer la base local. Reintentá sin borrar los datos.", error = true)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun aplicarPreferencias(datos: List<Recordatorio>, p: Preferencias): List<Recordatorio> {
        val visibles = datos.filter { p.mostrarCompletados || it.estado != EstadoRecordatorio.Completado }
        return when (p.orden) {
            Orden.Fecha -> visibles.sortedByDescending { it.fechaCreacion }
            Orden.Titulo -> visibles.sortedBy { it.titulo.lowercase() }
            Orden.Prioridad -> visibles.sortedByDescending { it.prioridad.nivel }
        }
    }
    override suspend fun guardarPreferencias(p: Preferencias) {
        try { ajustes.guardar(p) } catch (e: IOException) {
            estadoSync.value = Sincronizacion(mensaje = "No se pudieron guardar las preferencias. Reintentá.", error = true)
        }
    }
    override suspend fun obtener(escenario: Escenario): List<Recordatorio> {
        if (escenario == Escenario.Error) error("Error local simulado")
        if (escenario == Escenario.Carga) delay(2800)
        if (escenario == Escenario.Vacio) return emptyList()
        return aplicarPreferencias(dao.listar().map { it.aDominio() }, ajustes.flujo.first())
    }
    override suspend fun obtenerPorId(id: Int): Recordatorio? = dao.buscar(id)?.takeUnless { it.eliminado }?.aDominio()
    override fun observarPorId(id: Int): Flow<Recordatorio?> = dao.observarPorId(id).map { it?.aDominio() }

    override suspend fun guardar(nuevo: NuevoRecordatorio, simularError: Boolean): Recordatorio {
        if (simularError) throw IOException("Fallo local simulado")
        val entidad = RecordatorioEntity(remotoId = UUID.randomUUID().toString(), titulo = nuevo.titulo.trim(),
            descripcion = nuevo.descripcion, categoria = nuevo.categoria.etiqueta, prioridad = nuevo.prioridad.etiqueta,
            estado = nuevo.estado.etiqueta, fecha = LocalDateTime.now().toString())
        entidad.copy(id = 1).aDominio() // Comprobar las reglas antes de escribir.
        val id = dao.insertar(entidad).toInt()
        scope.launch { sincronizar() }
        return requireNotNull(dao.buscar(id)).aDominio()
    }
    override suspend fun actualizar(id: Int, nuevo: NuevoRecordatorio): Recordatorio {
        val resultado = db.withTransaction {
            val anterior = requireNotNull(dao.buscar(id)).also { require(!it.eliminado) }
            val cambiado = anterior.copy(titulo = nuevo.titulo.trim(), descripcion = nuevo.descripcion,
                categoria = nuevo.categoria.etiqueta, prioridad = nuevo.prioridad.etiqueta,
                estado = nuevo.estado.etiqueta, revision = anterior.revision + 1, sincronizacion = "Pendiente")
            cambiado.aDominio()
            dao.actualizar(cambiado)
            cambiado.aDominio()
        }
        scope.launch { sincronizar() }
        return resultado
    }
    override suspend fun eliminar(id: Int) {
        dao.marcarEliminado(id) // Oculto en el listado, conservado como operación pendiente.
        scope.launch { sincronizar() }
    }
    override fun simularSinRed(valor: Boolean) {
        sinRed.value = valor
        if (valor) estadoSync.value = Sincronizacion(mensaje = "Sin conexión simulada. Room conserva los cambios pendientes.")
        else scope.launch { sincronizar() }
    }

    override suspend fun sincronizar() {
        if (!bloqueoSync.tryLock()) return // No encolar varios envíos por eventos simultáneos.
        var repetir = false
        try {
            if (sinRed.value || !hayRed()) {
                estadoSync.value = Sincronizacion(mensaje = "Sin conexión. Los cambios locales quedan pendientes.")
                return
            }
            estadoSync.value = Sincronizacion(ocupada = true, mensaje = "Sincronizando…")
            for (fila in dao.pendientes()) {
                if (sinRed.value || !hayRed()) throw IOException("Sin conexión")
                try {
                    if (fila.eliminado) {
                        servicio.eliminar(fila.remotoId)
                        dao.borrarConfirmado(fila.id, fila.revision)
                    } else {
                        val respuesta = servicio.guardar(fila.remotoId, fila.aDto())
                        require(respuesta.id == fila.remotoId && respuesta.revision == fila.revision)
                        val confirmada = respuesta.aEntidad(fila.id)
                        db.withTransaction {
                            val actual = dao.buscar(fila.id)
                            if (actual != null && actual.revision == fila.revision && !actual.eliminado) dao.actualizar(confirmada)
                        }
                    }
                } catch (cancelada: CancellationException) { throw cancelada
                } catch (fallo: Exception) {
                    dao.confirmar(fila.id, fila.revision, "Error")
                    throw fallo
                }
            }
            val remotos = servicio.listar()
            db.withTransaction {
                remotos.forEach { dto ->
                    val actual = dao.buscarRemoto(dto.id)
                    if (actual == null) dao.insertar(dto.aEntidad())
                    else if (actual.sincronizacion == "Sincronizado" && !actual.eliminado && dto.revision >= actual.revision)
                        dao.actualizar(dto.aEntidad(actual.id))
                }
            }
            estadoSync.value = Sincronizacion(mensaje = "Sincronización completa. No se duplicaron los registros.")
            repetir = dao.pendientes().isNotEmpty()
        } catch (cancelada: CancellationException) { throw cancelada
        } catch (fallo: Exception) {
            android.util.Log.w("RecordatoriosSync", "Sincronización interrumpida: ${fallo.javaClass.simpleName}: ${fallo.message}")
            estadoSync.value = Sincronizacion(mensaje = mensajeFallo(fallo), error = true)
        } finally {
            bloqueoSync.unlock()
        }
        if (repetir) scope.launch { sincronizar() }
    }
}

fun mensajeFallo(fallo: Exception): String = when (fallo) {
    is HttpException -> when (fallo.code()) {
        in 400..499 -> "Solicitud inválida o en conflicto (HTTP ${fallo.code()}). Los datos locales se conservaron."
        in 500..599 -> "El servidor falló (HTTP ${fallo.code()}). Podés reintentar; los datos siguen en Room."
        else -> "Respuesta HTTP ${fallo.code()} no esperada. Reintentá."
    }
    is SocketTimeoutException -> "Tiempo de espera agotado. Los cambios locales se conservaron. Reintentá."
    is IOException -> "No hay conexión con el servicio. Los cambios quedan guardados para reintentar."
    is IllegalArgumentException, is IllegalStateException -> "El servicio devolvió datos inválidos. No se reemplazaron los datos locales."
    else -> "No se pudo completar la operación de datos. Se conservó la información local."
}
