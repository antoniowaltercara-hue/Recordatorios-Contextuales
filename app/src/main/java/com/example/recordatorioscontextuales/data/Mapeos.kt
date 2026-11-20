package com.example.recordatorioscontextuales.data

import com.example.recordatorioscontextuales.*
import com.example.recordatorioscontextuales.data.local.RecordatorioEntity
import com.example.recordatorioscontextuales.data.remote.RecordatorioDto
import java.time.LocalDateTime
import java.util.UUID

fun RecordatorioEntity.aDominio() = Recordatorio(
    id, titulo, descripcion,
    when(categoria) { "Personal" -> Categoria.Personal; "Trabajo" -> Categoria.Trabajo; "Salud" -> Categoria.Salud; "Estudios" -> Categoria.Estudios; else -> error("Categoría inválida") },
    when(prioridad) { "Baja" -> Prioridad.Baja; "Media" -> Prioridad.Media; "Alta" -> Prioridad.Alta; else -> error("Prioridad inválida") },
    when(estado) { "Pendiente" -> EstadoRecordatorio.Pendiente; "En curso" -> EstadoRecordatorio.EnCurso; "Completado" -> EstadoRecordatorio.Completado; else -> error("Estado inválido") },
    vencimiento?.let(LocalDateTime::parse),
    if (latitud != null && longitud != null) Ubicacion(latitud, longitud, lugar) else null,
    aviso?.let(LocalDateTime::parse), sincronizacion == "Sincronizado", LocalDateTime.parse(fecha), sincronizacion
)
fun RecordatorioEntity.aDto() = RecordatorioDto(remotoId, titulo, descripcion, categoria, prioridad,
    estado, fecha, revision, vencimiento, latitud, longitud, lugar, aviso)
fun RecordatorioDto.aEntidad(localId: Int = 0): RecordatorioEntity {
    UUID.fromString(id)
    require(revision > 0)
    val e = RecordatorioEntity(localId, id, titulo, descripcion, categoria, prioridad, estado,
        fecha, vencimiento, latitud, longitud, lugar, aviso, revision, "Sincronizado")
    e.copy(id = if (localId == 0) 1 else localId).aDominio() // El ID local real lo asigna Room al insertar.
    return e
}
fun Recordatorio.aEntidad() = RecordatorioEntity(id, UUID.randomUUID().toString(), titulo, descripcion,
    categoria.etiqueta, prioridad.etiqueta, estado.etiqueta, fechaCreacion.toString(),
    fechaVencimiento?.toString(), ubicacion?.latitud, ubicacion?.longitud, ubicacion?.descripcion, aviso?.toString())
