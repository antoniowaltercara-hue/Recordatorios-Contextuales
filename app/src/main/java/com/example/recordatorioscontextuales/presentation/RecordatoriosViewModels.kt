package com.example.recordatorioscontextuales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.recordatorioscontextuales.*
import com.example.recordatorioscontextuales.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T : ViewModel> factory(crear: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = crear() as V
}

class ListadoViewModel(private val repository: RecordatoriosRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<ListadoUiState>(ListadoUiState.Cargando)
    val uiState: StateFlow<ListadoUiState> = mutableState.asStateFlow()
    private var carga: Job? = null
    private var todos: List<Recordatorio> = emptyList()
    private var filtro: Filtro = Filtro.Todos

    init {
        cargar()
        viewModelScope.launch { repository.cambios.drop(1).collect { cargar() } }
    }

    fun cargar(escenario: Escenario = Escenario.Datos) {
        carga?.cancel()
        mutableState.update { ListadoUiState.Cargando }
        carga = viewModelScope.launch {
            try {
                todos = repository.obtener(escenario)
                publicarDatos()
            } catch (cancelada: CancellationException) { throw cancelada
            } catch (_: Exception) {
                mutableState.update { ListadoUiState.Error("No se pudieron cargar los recordatorios. Volvé a intentar.") }
            }
        }
    }

    fun seleccionarFiltro(nuevo: Filtro) {
        filtro = nuevo
        if (mutableState.value is ListadoUiState.Datos || mutableState.value is ListadoUiState.Vacio) publicarDatos()
    }

    private fun publicarDatos() {
        val visibles = when (filtro) {
            Filtro.Todos -> todos
            Filtro.Pendientes -> todos.filter { it.estado == EstadoRecordatorio.Pendiente }
            Filtro.Alta -> todos.filter { it.prioridad == Prioridad.Alta }
        }
        mutableState.update {
            if (visibles.isEmpty()) ListadoUiState.Vacio(
                if (todos.isEmpty()) "Todavía no existen recordatorios." else "No hay recordatorios para este filtro.", filtro
            ) else ListadoUiState.Datos(visibles.toList(), filtro, todos.map { it.categoria }.toSet().size)
        }
    }
}

class CreacionViewModel(private val repository: RecordatoriosRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CreacionUiState())
    val uiState: StateFlow<CreacionUiState> = mutableState.asStateFlow()
    private var envio: Job? = null

    private fun cambiar(transformar: (CreacionUiState) -> CreacionUiState) {
        if (mutableState.value.enviando) return
        mutableState.update {
            val nuevo = transformar(it)
            nuevo.copy(errores = ValidadorFormulario.validar(nuevo.titulo, nuevo.descripcion, nuevo.categoria, nuevo.prioridad),
                resultado = ResultadoGuardado.SinEnviar)
        }
    }
    fun titulo(valor: String) = cambiar { it.copy(titulo = valor) }
    fun descripcion(valor: String) = cambiar { it.copy(descripcion = valor) }
    fun categoria(valor: Categoria) = cambiar { it.copy(categoria = valor) }
    fun prioridad(valor: Prioridad) = cambiar { it.copy(prioridad = valor) }
    fun simularError(valor: Boolean) = cambiar { it.copy(simularError = valor) }

    fun guardar() {
        val actual = mutableState.value
        if (!actual.puedeGuardar) return
        mutableState.update { it.copy(resultado = ResultadoGuardado.Enviando) }
        envio = viewModelScope.launch {
            try {
                val creado = repository.guardar(NuevoRecordatorio(actual.titulo, actual.descripcion,
                    requireNotNull(actual.categoria), requireNotNull(actual.prioridad)), actual.simularError)
                mutableState.update { it.copy(resultado = ResultadoGuardado.Exito(creado.id)) }
            } catch (cancelada: CancellationException) { throw cancelada
            } catch (_: Exception) {
                mutableState.update { it.copy(resultado = ResultadoGuardado.Error(
                    "No se pudo guardar. Si activaste el error simulado, desactivalo y reintentá. Los campos se conservaron.")) }
            }
        }
    }

    fun cancelar() { envio?.cancel() }
}

class DetalleViewModel(private val repository: RecordatoriosRepository, private val id: Int) : ViewModel() {
    private val mutableState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = mutableState.asStateFlow()
    private var carga: Job? = null
    init { cargar() }
    fun cargar() {
        carga?.cancel()
        mutableState.update { DetalleUiState.Cargando }
        carga = viewModelScope.launch {
            try {
                val registro = repository.obtenerPorId(id)
                mutableState.update { if (registro == null) DetalleUiState.Error("El recordatorio no existe.") else DetalleUiState.Datos(registro) }
            } catch (cancelada: CancellationException) { throw cancelada
            } catch (_: Exception) { mutableState.update { DetalleUiState.Error("No se pudo abrir el recordatorio.") } }
        }
    }
}
