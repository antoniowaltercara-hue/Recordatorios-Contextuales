package com.example.recordatorioscontextuales

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime

object ReminderData {
    private val base = LocalDateTime.of(2026, 11, 10, 10, 0)
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(1, "Comprar alimentos", "Leche, pan y frutas",
            Categoria.Personal, Prioridad.Media,
            EstadoRecordatorio.Pendiente),
        Recordatorio(2, "Entregar informe", "Revisar antes de enviar",
            Categoria.Trabajo, Prioridad.Alta,
            EstadoRecordatorio.EnCurso, base.plusDays(1)),
        Recordatorio(3, "Turno médico", "Control de rutina",
            Categoria.Salud, Prioridad.Alta,
            EstadoRecordatorio.Pendiente, base.plusDays(2),
            Ubicacion(-26.8241, -65.2226, "Centro médico"),
            base.plusDays(2).minusHours(1)),
        Recordatorio(4, "Estudiar Kotlin", "Repasar colecciones",
            Categoria.Estudios, Prioridad.Baja,
            EstadoRecordatorio.Completado),
        Recordatorio(5, "Reunión de equipo", "Revisar el proyecto",
            Categoria.Trabajo, Prioridad.Media,
            EstadoRecordatorio.Pendiente, base.plusDays(3))
    )
    val categorias: Set<Categoria> =
        recordatorios.map { it.categoria }.toSet()
    val estiloCategoria: Map<Categoria, Pair<String, Color>> = mapOf(
        Categoria.Personal to ("Personal" to Color(0xFF1976D2)),
        Categoria.Trabajo to ("Trabajo" to Color(0xFF7B1FA2)),
        Categoria.Salud to ("Salud" to Color(0xFF00695C)),
        Categoria.Estudios to ("Estudios" to Color(0xFF9C4F00))
    )
    fun porEstado(estado: EstadoRecordatorio): List<Recordatorio> =
        recordatorios.filter { it.estado == estado }
    fun porPrioridad(prioridad: Prioridad): List<Recordatorio> =
        recordatorios.filter { it.prioridad == prioridad }
}
