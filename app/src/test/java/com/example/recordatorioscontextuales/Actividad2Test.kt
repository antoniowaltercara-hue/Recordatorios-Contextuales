package com.example.recordatorioscontextuales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.example.recordatorioscontextuales.data.*
import com.example.recordatorioscontextuales.presentation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Actividad2Test {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var contador = 0
    private fun <T : ViewModel> registrar(vm: T): T { store.put("vm${contador++}", vm); return vm }
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun cerrar() { store.clear(); Dispatchers.resetMain() }
    private fun repository(iniciales: List<Recordatorio> = ReminderData.recordatorios) = InMemoryRecordatoriosRepository(iniciales, 100)
    private fun completar(vm: CreacionViewModel) {
        vm.titulo("  Revisar entrega  ")
        vm.descripcion("Verificar las evidencias")
        vm.categoria(Categoria.Estudios)
        vm.prioridad(Prioridad.Alta)
    }

    @Test fun validacionesContemplanLosLimitesYSelecciones() {
        assertFalse(ValidadorFormulario.validar(" abc ", "", Categoria.Personal, Prioridad.Baja).esValido)
        assertTrue(ValidadorFormulario.validar(" abcd ", "x".repeat(200), Categoria.Personal, Prioridad.Baja).esValido)
        assertNotNull(ValidadorFormulario.validar("abcd", "x".repeat(201), Categoria.Personal, Prioridad.Baja).descripcion)
        val faltantes = ValidadorFormulario.validar("abcd", "", null, null)
        assertNotNull(faltantes.categoria)
        assertNotNull(faltantes.prioridad)
    }
    @Test fun modeloTambienRechazaTituloCortoYDescripcionExcesiva() {
        val original = ReminderData.recordatorios.first()
        assertThrows(IllegalArgumentException::class.java) { original.copy(titulo = " abc ") }
        assertThrows(IllegalArgumentException::class.java) { original.copy(descripcion = "x".repeat(201)) }
    }
    @Test fun listadoCargaDatosVacioErrorYReintentaSinBorrar() = runTest(dispatcher) {
        val repo = repository()
        val vm = registrar(ListadoViewModel(repo))
        assertTrue(vm.uiState.value is ListadoUiState.Cargando)
        advanceUntilIdle()
        assertEquals(5, (vm.uiState.value as ListadoUiState.Datos).recordatorios.size)
        vm.cargar(Escenario.Vacio); advanceUntilIdle()
        assertTrue(vm.uiState.value is ListadoUiState.Vacio)
        assertEquals(5, repo.cambios.value.size)
        vm.cargar(Escenario.Error); advanceUntilIdle()
        assertTrue(vm.uiState.value is ListadoUiState.Error)
        vm.cargar(); advanceUntilIdle()
        assertTrue(vm.uiState.value is ListadoUiState.Datos)
    }
    @Test fun unaCargaAntiguaNoReemplazaLaSeleccionMasReciente() = runTest(dispatcher) {
        val vm = registrar(ListadoViewModel(repository()))
        vm.cargar(Escenario.Carga); runCurrent()
        vm.cargar(Escenario.Error); advanceUntilIdle()
        assertTrue(vm.uiState.value is ListadoUiState.Error)
    }
    @Test fun formularioInvalidoNoGuarda() = runTest(dispatcher) {
        val repo = repository()
        val vm = registrar(CreacionViewModel(repo))
        assertFalse(vm.uiState.value.puedeGuardar)
        vm.guardar(); advanceUntilIdle()
        assertEquals(5, repo.cambios.value.size)
        assertEquals(ResultadoGuardado.SinEnviar, vm.uiState.value.resultado)
    }
    @Test fun guardaUnaSolaVezNormalizaTituloYActualizaListado() = runTest(dispatcher) {
        val repo = repository()
        val lista = registrar(ListadoViewModel(repo)); advanceUntilIdle()
        val vm = registrar(CreacionViewModel(repo)); completar(vm)
        vm.guardar(); vm.guardar()
        assertTrue(vm.uiState.value.enviando)
        assertFalse(vm.uiState.value.puedeGuardar)
        vm.titulo("Cambio durante envío")
        advanceUntilIdle()
        val exito = vm.uiState.value.resultado as ResultadoGuardado.Exito
        assertEquals(6, repo.cambios.value.size)
        assertEquals("Revisar entrega", repo.obtenerPorId(exito.id)?.titulo)
        assertEquals(6, (lista.uiState.value as ListadoUiState.Datos).recordatorios.size)
        assertEquals("  Revisar entrega  ", vm.uiState.value.titulo)
    }
    @Test fun errorConservaCamposYPermiteReintento() = runTest(dispatcher) {
        val repo = repository(); val vm = registrar(CreacionViewModel(repo)); completar(vm)
        vm.simularError(true); vm.guardar(); advanceUntilIdle()
        assertTrue(vm.uiState.value.resultado is ResultadoGuardado.Error)
        assertEquals(5, repo.cambios.value.size)
        assertEquals("  Revisar entrega  ", vm.uiState.value.titulo)
        vm.simularError(false); vm.guardar(); advanceUntilIdle()
        assertTrue(vm.uiState.value.resultado is ResultadoGuardado.Exito)
        assertEquals(6, repo.cambios.value.size)
    }
    @Test fun cancelarDuranteEnvioNoAgregaUnRegistro() = runTest(dispatcher) {
        val repo = repository(); val vm = registrar(CreacionViewModel(repo)); completar(vm)
        vm.guardar(); runCurrent(); vm.cancelar(); advanceUntilIdle()
        assertEquals(5, repo.cambios.value.size)
    }
    @Test fun detalleResuelveIdYComunicaInexistente() = runTest(dispatcher) {
        val repo = repository()
        val existente = registrar(DetalleViewModel(repo, 3))
        val ausente = registrar(DetalleViewModel(repo, 999))
        advanceUntilIdle()
        assertEquals("Turno médico", (existente.uiState.value as DetalleUiState.Datos).recordatorio.titulo)
        assertTrue(ausente.uiState.value is DetalleUiState.Error)
    }
    @Test fun repositorioRealmenteVacioPermiteCrearElPrimero() = runTest(dispatcher) {
        val repo = repository(emptyList())
        assertTrue(repo.obtener().isEmpty())
        val creado = repo.guardar(NuevoRecordatorio("Primero", "", Categoria.Personal, Prioridad.Baja))
        assertEquals(1, creado.id)
        assertEquals(EstadoRecordatorio.Pendiente, creado.estado)
    }
}
