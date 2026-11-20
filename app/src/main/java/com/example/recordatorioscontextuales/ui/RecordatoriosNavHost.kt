package com.example.recordatorioscontextuales.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.recordatorioscontextuales.data.RecordatoriosRepository
import com.example.recordatorioscontextuales.data.RepositorioPersistente
import kotlinx.coroutines.launch
import com.example.recordatorioscontextuales.presentation.*

object Rutas {
    const val LISTADO = "listado"
    const val CREAR = "crear"
    const val DETALLE = "detalle/{id}"
    const val EDITAR = "editar/{id}"
    const val AJUSTES = "ajustes"
    fun detalle(id: Int) = "detalle/$id"
}

@Composable
fun RecordatoriosNavHost(repository: RecordatoriosRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Rutas.LISTADO) {
        composable(Rutas.LISTADO) {
            val vm: ListadoViewModel = viewModel(factory = factory { ListadoViewModel(repository) })
            val estado by vm.uiState.collectAsStateWithLifecycle()
            PantallaListado(estado,
                onCrearRecordatorio = { navController.navigate(Rutas.CREAR) { launchSingleTop = true } },
                onRecordatorioClick = { navController.navigate(Rutas.detalle(it)) { launchSingleTop = true } },
                onReintentar = { vm.cargar() }, onFiltro = vm::seleccionarFiltro, onSimular = vm::cargar,
                onAjustes = { navController.navigate(Rutas.AJUSTES) { launchSingleTop = true } })
        }
        composable(Rutas.CREAR) {
            val vm: CreacionViewModel = viewModel(factory = factory { CreacionViewModel(repository) })
            val estado by vm.uiState.collectAsStateWithLifecycle()
            val cancelar: () -> Unit = { vm.cancelar(); navController.popBackStack(); Unit }
            BackHandler { cancelar() }
            LaunchedEffect(estado.resultado) {
                if (estado.resultado is ResultadoGuardado.Exito) navController.popBackStack()
            }
            PantallaCreacion(estado, vm::titulo, vm::descripcion, vm::categoria, vm::prioridad,
                onGuardar = vm::guardar, onCancelar = cancelar, onSimularError = vm::simularError)
        }
        composable(Rutas.EDITAR, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val vm: CreacionViewModel = viewModel(factory = factory { CreacionViewModel(repository, id) })
            val estado by vm.uiState.collectAsStateWithLifecycle()
            val cancelar: () -> Unit = { vm.cancelar(); navController.popBackStack(); Unit }
            BackHandler { cancelar() }
            LaunchedEffect(estado.resultado) { if (estado.resultado is ResultadoGuardado.Exito) navController.popBackStack() }
            PantallaCreacion(estado, vm::titulo, vm::descripcion, vm::categoria, vm::prioridad,
                vm::guardar, cancelar, vm::simularError, onEstado = vm::estado)
        }
        composable(Rutas.AJUSTES) {
            if (repository is RepositorioPersistente) {
                val vm: AjustesViewModel = viewModel(factory = factory { AjustesViewModel(repository) })
                PantallaAjustes(vm) { navController.popBackStack() }
            }
        }
        composable(Rutas.DETALLE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val vm: DetalleViewModel = viewModel(factory = factory { DetalleViewModel(repository, id) })
            val estado by vm.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var mensaje by remember { mutableStateOf<String?>(null) }
            PantallaDetalle(estado, onVolver = { navController.popBackStack() }, onReintentar = vm::cargar,
                onEditar = { navController.navigate("editar/$id") }, onEliminar = {
                    scope.launch {
                        try { vm.eliminar(); navController.popBackStack() }
                        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; mensaje = "No se pudo eliminar. El registro se conservó." }
                    }
                }, mensaje = mensaje)
        }
    }
}
