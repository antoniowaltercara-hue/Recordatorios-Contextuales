package com.example.recordatorioscontextuales.data

import com.example.recordatorioscontextuales.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class Escenario { Datos, Carga, Vacio, Error }

data class NuevoRecordatorio(
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val prioridad: Prioridad
)

interface RecordatoriosRepository {
    val cambios: StateFlow<List<Recordatorio>>
    suspend fun obtener(escenario: Escenario = Escenario.Datos): List<Recordatorio>
    suspend fun obtenerPorId(id: Int): Recordatorio?
    suspend fun guardar(nuevo: NuevoRecordatorio, simularError: Boolean = false): Recordatorio
}

class InMemoryRecordatoriosRepository(
    iniciales: List<Recordatorio> = ReminderData.recordatorios,
    private val demoraMillis: Long = 700
) : RecordatoriosRepository {
    private val registros = MutableStateFlow(iniciales.toList())
    override val cambios: StateFlow<List<Recordatorio>> = registros.asStateFlow()
    private val mutex = Mutex()

    override suspend fun obtener(escenario: Escenario): List<Recordatorio> {
        delay(if (escenario == Escenario.Carga) demoraMillis * 4 else demoraMillis)
        return when (escenario) {
            Escenario.Error -> throw IllegalStateException("Error de carga simulado. Podés reintentar.")
            Escenario.Vacio -> emptyList() // La simulación no elimina los datos de la sesión.
            Escenario.Carga, Escenario.Datos -> registros.value.toList()
        }
    }

    override suspend fun obtenerPorId(id: Int): Recordatorio? {
        delay(demoraMillis)
        return registros.value.find { it.id == id }
    }

    override suspend fun guardar(nuevo: NuevoRecordatorio, simularError: Boolean): Recordatorio {
        delay(demoraMillis * 2)
        if (simularError) throw IllegalStateException("No se pudo guardar: error simulado. Desactivá la simulación y reintentá.")
        return mutex.withLock {
            val recordatorio = Recordatorio(
                id = (registros.value.maxOfOrNull { it.id } ?: 0) + 1,
                titulo = nuevo.titulo.trim(), descripcion = nuevo.descripcion,
                categoria = nuevo.categoria, prioridad = nuevo.prioridad,
                estado = EstadoRecordatorio.Pendiente
            )
            registros.value = registros.value + recordatorio
            recordatorio
        }
    }
}
