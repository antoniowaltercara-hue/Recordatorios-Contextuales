package com.example.recordatorioscontextuales

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class Actividad2PantallasTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun esperar(texto: String) {
        compose.waitUntil(12_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun inicio() = esperar("Comprar alimentos")
    private fun captura(nombre: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val archivo = File(instrumentation.targetContext.getExternalFilesDir(null), "$nombre.png")
        archivo.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun formulario(titulo: String) {
        compose.onNodeWithContentDescription("Crear recordatorio").performClick()
        compose.onNodeWithTag("titulo").performTextInput(titulo)
        compose.onNodeWithTag("descripcion").performTextInput("Revisar modelos y navegación")
        compose.onNodeWithText("Estudios", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Alta", useUnmergedTree = true).performScrollTo().performClick()
    }
    @Test fun navegaAlDetalleYVuelveSinDuplicarElListado() {
        inicio(); captura("listado")
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Turno médico"))
        compose.onNodeWithText("Turno médico").performClick()
        esperar("Detalle del recordatorio")
        esperar("Identificador: 3")
        compose.onNodeWithText("Descripción: Control de rutina").assertIsDisplayed()
        captura("detalle")
        compose.onNodeWithText("Volver al listado").performScrollTo().performClick()
        inicio()
        compose.onAllNodesWithText("Mis recordatorios").assertCountEquals(1)
    }
    @Test fun validaCreaYConsultaElNuevoRecordatorio() {
        inicio()
        compose.onNodeWithContentDescription("Crear recordatorio").performClick()
        compose.onNodeWithTag("guardar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("titulo").performScrollTo().performTextInput(" abc ")
        compose.onNodeWithText("Escribí al menos 4 caracteres sin contar espacios de los extremos.").assertExists()
        compose.onNodeWithTag("titulo").performTextClearance()
        compose.onNodeWithTag("titulo").performTextInput("Preparar actividad dos")
        compose.onNodeWithTag("descripcion").performTextInput("Verificar la entrega")
        compose.onNodeWithText("Estudios", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Alta", useUnmergedTree = true).performScrollTo().performClick()
        captura("formulario")
        compose.onNodeWithTag("guardar").performScrollTo().assertIsEnabled().performClick()
        esperar("Mis recordatorios"); inicio()
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Preparar actividad dos"))
        compose.onNodeWithText("Preparar actividad dos").performClick()
        esperar("Descripción: Verificar la entrega")
        compose.onNodeWithText("Categoría: Estudios").assertIsDisplayed()
    }
    @Test fun simulacionesVacioCargaErrorYReintento() {
        inicio()
        compose.onNodeWithText("Simular estados").performClick()
        compose.onNodeWithText("Vacío").performClick()
        esperar("Todavía no existen recordatorios.")
        compose.onNodeWithText("Crear el primero").assertIsDisplayed(); captura("vacio")
        compose.onNodeWithText("Carga").performClick()
        esperar("Cargando recordatorios…"); captura("carga")
        inicio()
        compose.onNodeWithText("Error", substring = false).performClick()
        esperar("No se pudieron cargar los recordatorios. Volvé a intentar.")
        compose.onNodeWithText("Reintentar").assertIsDisplayed(); captura("error")
        compose.onNodeWithText("Reintentar").performClick(); inicio()
    }
    @Test fun falloDeGuardadoConservaFormularioYSePuedeReintentar() {
        inicio(); formulario("Prueba de reintento")
        compose.onNodeWithContentDescription("Simular error de guardado").performScrollTo().performClick()
        compose.onNodeWithTag("guardar").performScrollTo().performClick()
        esperar("No se pudo guardar. Si activaste el error simulado, desactivalo y reintentá. Los campos se conservaron.")
        compose.onNodeWithTag("titulo").performScrollTo().assertTextContains("Prueba de reintento")
        compose.onNodeWithContentDescription("Simular error de guardado").performScrollTo().performClick()
        compose.onNodeWithTag("guardar").performScrollTo().performClick()
        esperar("Mis recordatorios"); inicio()
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Prueba de reintento"))
        compose.onNodeWithText("Prueba de reintento").assertIsDisplayed()
    }
    @Test fun cancelarNoGuardaYElFormularioSobreviveRecreacion() {
        inicio(); formulario("Borrador cancelado")
        compose.activityRule.scenario.recreate()
        esperar("Nuevo recordatorio")
        compose.onNodeWithTag("titulo").assertTextContains("Borrador cancelado")
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        inicio()
        val repo = (compose.activity.application as RecordatoriosApplication).repository
        assertEquals(0, repo.cambios.value.count { it.titulo == "Borrador cancelado" })
    }
}
