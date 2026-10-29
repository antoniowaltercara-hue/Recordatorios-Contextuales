package com.example.recordatorioscontextuales

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class RecordatorioTest {
    private fun valido() = Recordatorio(1, "Estudiar", "Kotlin",
        Categoria.Estudios, Prioridad.Alta, EstadoRecordatorio.Pendiente)

    @Test fun aceptaDatosOpcionalesAusentes() {
        val r = valido()
        assertNull(r.ubicacion)
        assertNull(r.fechaVencimiento)
        assertNull(r.aviso)
    }
    @Test fun rechazaTituloEnBlanco() {
        assertThrows(IllegalArgumentException::class.java) { valido().copy(titulo = "  ") }
    }
    @Test fun rechazaIdNoPositivo() {
        listOf(0, -1).forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { valido().copy(id = id) }
        }
    }
    @Test fun validaCoordenadas() {
        assertThrows(IllegalArgumentException::class.java) { Ubicacion(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { Ubicacion(0.0, -181.0) }
        assertEquals(90.0, Ubicacion(90.0, 180.0).latitud, 0.0)
    }
    @Test fun rechazaAvisoPosteriorAlVencimiento() {
        val fecha = LocalDateTime.of(2026, 11, 10, 10, 0)
        assertThrows(IllegalArgumentException::class.java) {
            valido().copy(fechaVencimiento = fecha, aviso = fecha.plusHours(1))
        }
        assertEquals(fecha, valido().copy(fechaVencimiento = fecha, aviso = fecha).aviso)
    }
    @Test fun copyConservaElOriginal() {
        val original = valido()
        val modificado = original.copy(estado = EstadoRecordatorio.Completado)
        assertEquals(EstadoRecordatorio.Pendiente, original.estado)
        assertEquals(EstadoRecordatorio.Completado, modificado.estado)
    }
    @Test fun coleccionesYFiltrosCoincidenConLosEjemplos() {
        assertEquals(5, ReminderData.recordatorios.size)
        assertEquals(4, ReminderData.categorias.size)
        assertEquals(setOf(1, 3, 5), ReminderData.porEstado(EstadoRecordatorio.Pendiente).map { it.id }.toSet())
        assertEquals(setOf(2, 3), ReminderData.porPrioridad(Prioridad.Alta).map { it.id }.toSet())
        assertEquals(ReminderData.categorias, ReminderData.estiloCategoria.keys)
    }
}
