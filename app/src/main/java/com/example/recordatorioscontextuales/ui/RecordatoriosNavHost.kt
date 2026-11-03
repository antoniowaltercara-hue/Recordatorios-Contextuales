package com.example.recordatorioscontextuales.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.recordatorioscontextuales.data.RecordatoriosRepository
import com.example.recordatorioscontextuales.presentation.*

object Rutas {
    const val LISTADO = "listado"
    const val CREAR = "crear"
    const val DETALLE = "detalle/{id}"
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
                onReintentar = { vm.cargar() }, onFiltro = vm::seleccionarFiltro, onSimular = vm::cargar)
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
        composable(Rutas.DETALLE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val vm: DetalleViewModel = viewModel(factory = factory { DetalleViewModel(repository, id) })
            val estado by vm.uiState.collectAsStateWithLifecycle()
            PantallaDetalle(estado, onVolver = { navController.popBackStack() }, onReintentar = vm::cargar)
        }
    }
}
