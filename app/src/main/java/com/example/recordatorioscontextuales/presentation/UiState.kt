package com.example.recordatorioscontextuales.presentation

import com.example.recordatorioscontextuales.*

val categoriasPermitidas: List<Categoria> = listOf(Categoria.Personal, Categoria.Trabajo, Categoria.Salud, Categoria.Estudios)
val prioridadesPermitidas: List<Prioridad> = listOf(Prioridad.Baja, Prioridad.Media, Prioridad.Alta)

enum class Filtro(val etiqueta: String) { Todos("Todos"), Pendientes("Pendientes"), Alta("Alta prioridad") }

sealed interface ListadoUiState {
    data object Cargando : ListadoUiState
    data class Vacio(val mensaje: String = "Todavía no existen recordatorios.", val filtro: Filtro = Filtro.Todos) : ListadoUiState
    data class Datos(val recordatorios: List<Recordatorio>, val filtro: Filtro, val categorias: Int) : ListadoUiState
    data class Error(val mensaje: String) : ListadoUiState
}

data class ErroresFormulario(
    val titulo: String? = null, val descripcion: String? = null,
    val categoria: String? = null, val prioridad: String? = null
) {
    val esValido: Boolean get() = listOf(titulo, descripcion, categoria, prioridad).all { it == null }
}

object ValidadorFormulario {
    fun validar(titulo: String, descripcion: String, categoria: Categoria?, prioridad: Prioridad?) = ErroresFormulario(
        titulo = if (titulo.trim().length < 4) "Escribí al menos 4 caracteres sin contar espacios de los extremos." else null,
        descripcion = if (descripcion.length > 200) "La descripción supera los 200 caracteres." else null,
        categoria = if (categoria !in categoriasPermitidas) "Seleccioná una categoría válida." else null,
        prioridad = if (prioridad !in prioridadesPermitidas) "Seleccioná una prioridad." else null
    )
}

sealed interface ResultadoGuardado {
    data object SinEnviar : ResultadoGuardado
    data object Enviando : ResultadoGuardado
    data class Exito(val id: Int) : ResultadoGuardado
    data class Error(val mensaje: String) : ResultadoGuardado
}

data class CreacionUiState(
    val titulo: String = "", val descripcion: String = "",
    val categoria: Categoria? = null, val prioridad: Prioridad? = null,
    val errores: ErroresFormulario = ValidadorFormulario.validar("", "", null, null),
    val resultado: ResultadoGuardado = ResultadoGuardado.SinEnviar,
    val simularError: Boolean = false
) {
    val enviando: Boolean get() = resultado is ResultadoGuardado.Enviando
    val puedeGuardar: Boolean get() = errores.esValido && !enviando && resultado !is ResultadoGuardado.Exito
}

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Datos(val recordatorio: Recordatorio) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
