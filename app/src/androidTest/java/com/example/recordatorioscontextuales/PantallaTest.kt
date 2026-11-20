package com.example.recordatorioscontextuales

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class PantallaTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun muestraLosDatosYPermiteDesplazarse() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("lista").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Comprar alimentos"))
        compose.onNodeWithText("Mis recordatorios").assertIsDisplayed()
        compose.onNodeWithText("Comprar alimentos").assertIsDisplayed()
        compose.onNodeWithText("Categoría: Personal").assertIsDisplayed()
        compose.onAllNodesWithText("Prioridad: Media")[0].assertIsDisplayed()
        compose.onAllNodesWithText("Estado: Pendiente")[0].assertIsDisplayed()
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Reunión de equipo"))
        compose.onNodeWithText("Reunión de equipo").assertIsDisplayed()
    }
    @Test fun filtraPorEstadoYPrioridad() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("lista").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Comprar alimentos"))
        compose.onNodeWithText("Pendientes").performClick()
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Comprar alimentos"))
        compose.onNodeWithText("Entregar informe").assertDoesNotExist()
        compose.onNodeWithText("Comprar alimentos").assertIsDisplayed()
        compose.onNodeWithText("Alta prioridad").performClick()
        compose.onNodeWithText("Comprar alimentos").assertDoesNotExist()
        compose.onNodeWithText("Entregar informe").assertIsDisplayed()
        compose.onNodeWithText("Turno médico").assertIsDisplayed()
        compose.onNodeWithText("Todos").performClick()
        compose.onNodeWithTag("lista").performScrollToNode(hasText("Comprar alimentos"))
        compose.onNodeWithText("Comprar alimentos").assertIsDisplayed()
    }
}
