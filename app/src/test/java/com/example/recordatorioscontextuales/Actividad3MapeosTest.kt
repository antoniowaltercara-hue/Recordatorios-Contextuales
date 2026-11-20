package com.example.recordatorioscontextuales

import androidx.datastore.preferences.core.*
import com.example.recordatorioscontextuales.data.*
import com.example.recordatorioscontextuales.data.local.*
import org.junit.Assert.*
import org.junit.Test

class Actividad3MapeosTest {
    @Test fun idaYVueltaConservaDatosYOpcionales() {
        val r = ReminderData.recordatorios[2]
        val e = r.aEntidad()
        val copia = e.aDto().aEntidad(r.id).aDominio()
        assertEquals(r.id, copia.id)
        assertEquals(r.titulo, copia.titulo)
        assertEquals(r.ubicacion, copia.ubicacion)
        assertEquals(r.aviso, copia.aviso)
        assertTrue(copia.sincronizado)
    }
    @Test fun dtoConIdOCategoriaInvalidaSeRechaza() {
        val dto = ReminderData.recordatorios[0].aEntidad().aDto()
        assertThrows(IllegalArgumentException::class.java) { dto.copy(id = "incorrecto").aEntidad() }
        assertThrows(IllegalStateException::class.java) { dto.copy(categoria = "inventada").aEntidad(1) }
    }
    @Test fun defaultsNoDependendeQueExistanClaves() {
        assertEquals(Preferencias(), PreferenciasRepository.interpretar(emptyPreferences()))
        assertEquals(Orden.Fecha, PreferenciasRepository.interpretar(preferencesOf(PreferenciasRepository.ORDEN to "invalido")).orden)
        assertFalse(PreferenciasRepository.interpretar(preferencesOf(stringPreferencesKey("tema_oscuro") to "no_booleano")).oscuro)
    }
}
