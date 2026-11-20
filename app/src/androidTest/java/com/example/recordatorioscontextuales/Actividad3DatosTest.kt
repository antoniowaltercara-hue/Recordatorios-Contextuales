package com.example.recordatorioscontextuales

import android.content.Context
import androidx.room.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.recordatorioscontextuales.data.*
import com.example.recordatorioscontextuales.data.local.*
import com.example.recordatorioscontextuales.data.remote.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Actividad3DatosTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = Gson()
    private lateinit var server: MockWebServer
    private lateinit var db: BaseDatos
    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var repo: LocalFirstRepository
    private lateinit var nombreDb: String
    private lateinit var archivoPrefs: File
    private val remotos = ConcurrentHashMap<String, RecordatorioDto>()
    private val puts = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var modo = "normal"
    @Volatile private var conectado = true

    @Before fun preparar() {
        nombreDb = "prueba-${UUID.randomUUID()}.db"
        archivoPrefs = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (modo == "500" || modo == "400") return MockResponse().setResponseCode(modo.toInt())
                val id = request.path!!.substringAfterLast('/')
                val respuesta = when (request.method) {
                    "PUT" -> {
                        puts.incrementAndGet()
                        val dto = gson.fromJson(request.body.readUtf8(), RecordatorioDto::class.java)
                        remotos[id] = dto
                        MockResponse().setBody(gson.toJson(dto))
                    }
                    "DELETE" -> { remotos.remove(id); MockResponse().setResponseCode(204) }
                    else -> if (modo == "invalido") MockResponse().setBody("[{\"id\":\"incorrecto\"}]")
                        else MockResponse().setBody(gson.toJson(remotos.values.toList()))
                }
                if (modo == "timeout") respuesta.setBodyDelay(1500, TimeUnit.MILLISECONDS)
                if (modo == "lento") respuesta.setBodyDelay(300, TimeUnit.MILLISECONDS)
                return respuesta.addHeader("Content-Type", "application/json")
            }
        }
        server.start()
        abrir()
    }
    private fun abrir() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        db = Room.databaseBuilder(context, BaseDatos::class.java, nombreDb).build()
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { archivoPrefs })
        repo = LocalFirstRepository(db, PreferenciasRepository(store), ServicioFactory.crear(server.url("/").toString(), 1), scope) { conectado }
        repo.simularSinRed(true)
    }
    private suspend fun cerrar() { scope.coroutineContext.job.cancelAndJoin(); db.close() }
    @After fun limpiar() = runBlocking {
        cerrar(); server.shutdown(); context.deleteDatabase(nombreDb); archivoPrefs.delete(); Unit
    }
    private fun datos(titulo: String = "Registro persistente") = NuevoRecordatorio(titulo, "Creado sin red", Categoria.Estudios, Prioridad.Alta)
    private suspend fun esperar(condicion: suspend () -> Boolean) {
        withTimeout(12_000) { while (!condicion()) delay(30) }
    }

    @Test fun conservaRoomAlCerrarReabrirYActualizaElimina() = runBlocking {
        val creado = repo.guardar(datos())
        val uuid = db.recordatorios().buscar(creado.id)!!.remotoId
        cerrar(); abrir()
        assertEquals("Registro persistente", repo.obtenerPorId(creado.id)?.titulo)
        repo.actualizar(creado.id, datos("Registro actualizado").copy(estado = EstadoRecordatorio.Completado))
        assertEquals(creado.id, repo.obtenerPorId(creado.id)?.id)
        assertEquals(1, db.recordatorios().filtrarPorEstado("Completado").first().size)
        assertEquals(uuid, db.recordatorios().buscar(creado.id)!!.remotoId)
        repo.eliminar(creado.id)
        assertNull(repo.obtenerPorId(creado.id))
        assertTrue(db.recordatorios().buscar(creado.id)!!.eliminado)
        assertEquals(0, repo.obtener().size)
    }
    @Test fun conservaTresPreferenciasYAplicaOrdenYVisibilidad() = runBlocking {
        repo.guardar(datos("Zeta pendiente"))
        val completa = repo.guardar(datos("Alfa completada"))
        repo.actualizar(completa.id, datos("Alfa completada").copy(estado = EstadoRecordatorio.Completado))
        repo.guardarPreferencias(Preferencias(true, false, Orden.Titulo))
        cerrar(); abrir()
        val p = PreferenciasRepository(store).flujo.first()
        assertEquals(Preferencias(true, false, Orden.Titulo), p)
        assertEquals(listOf("Zeta pendiente"), repo.obtener().map { it.titulo })
        repo.guardarPreferencias(p.copy(mostrarCompletados = true))
        assertEquals(listOf("Alfa completada", "Zeta pendiente"), repo.obtener().map { it.titulo })
    }
    @Test fun preferenciasAusentesYDesconocidasUsanPredeterminados() = runBlocking {
        assertEquals(Preferencias(), PreferenciasRepository(store).flujo.first())
        store.edit { it[PreferenciasRepository.ORDEN] = "opcion_desconocida" }
        assertEquals(Orden.Fecha, PreferenciasRepository(store).flujo.first().orden)
    }
    @Test fun sinConexionRealLaInterfazSigueLeyendoRoom() = runBlocking {
        conectado = false
        val creado = repo.guardar(datos())
        repo.simularSinRed(false); repo.sincronizar()
        assertEquals(creado.id, repo.obtener().single().id)
        assertEquals("Pendiente", db.recordatorios().buscar(creado.id)?.sincronizacion)
        assertEquals(0, puts.get())
    }
    @Test fun llamadasConcurrentesYReintentosNoDuplican() = runBlocking {
        val creado = repo.guardar(datos())
        modo = "lento"; repo.simularSinRed(false)
        coroutineScope { repeat(20) { launch(Dispatchers.IO) { repo.sincronizar() } } }
        esperar { db.recordatorios().buscar(creado.id)?.sincronizacion == "Sincronizado" }
        modo = "normal"; repo.sincronizar(); repo.sincronizar()
        assertEquals(1, puts.get()); assertEquals(1, remotos.size); assertEquals(1, repo.obtener().size)
    }
    @Test fun timeoutConservaDatosYReintentoUsaElMismoUuid() = runBlocking {
        val creado = repo.guardar(datos())
        val uuid = db.recordatorios().buscar(creado.id)!!.remotoId
        modo = "timeout"; repo.simularSinRed(false)
        esperar { repo.sincronizacion.value.error }
        assertTrue(repo.sincronizacion.value.mensaje.contains("espera"))
        assertEquals("Error", db.recordatorios().buscar(creado.id)?.sincronizacion)
        modo = "normal"; repo.sincronizar()
        esperar { db.recordatorios().buscar(creado.id)?.sincronizacion == "Sincronizado" }
        assertEquals(setOf(uuid), remotos.keys); assertEquals(1, repo.obtener().size)
    }
    @Test fun erroresHttp400Y500NoBorranLosDatos() = runBlocking {
        val creado = repo.guardar(datos())
        modo = "400"; repo.simularSinRed(false)
        esperar { repo.sincronizacion.value.mensaje.contains("400") }
        assertNotNull(repo.obtenerPorId(creado.id))
        modo = "500"; repo.sincronizar()
        assertTrue(repo.sincronizacion.value.mensaje.contains("500"))
        assertEquals("Error", db.recordatorios().buscar(creado.id)?.sincronizacion)
        modo = "normal"; repo.sincronizar()
        assertEquals("Sincronizado", db.recordatorios().buscar(creado.id)?.sincronizacion)
    }
    @Test fun descargaDtoValidoLoMapeaYNoLoDuplica() = runBlocking {
        val uuid = UUID.randomUUID().toString()
        remotos[uuid] = RecordatorioDto(uuid, "Registro remoto", "Desde el servicio", "Trabajo", "Media", "Pendiente", "2026-11-19T10:00:00", 1)
        repo.simularSinRed(false)
        esperar { repo.obtener().size == 1 }
        repo.sincronizar()
        assertEquals("Registro remoto", repo.obtener().single().titulo)
        assertEquals(1, repo.obtener().size)
    }
    @Test fun dtoInvalidoSeRechazaSinPerderRegistrosLocales() = runBlocking {
        val creado = repo.guardar(datos())
        repo.simularSinRed(false)
        esperar { db.recordatorios().buscar(creado.id)?.sincronizacion == "Sincronizado" }
        esperar { !repo.sincronizacion.value.ocupada }
        modo = "invalido"; repo.sincronizar()
        assertTrue(repo.sincronizacion.value.error)
        assertEquals(1, repo.obtener().size)
    }
    @Test fun edicionDuranteEnvioNoRecibeConfirmacionDeUnaVersionAntigua() = runBlocking {
        val creado = repo.guardar(datos())
        modo = "lento"; repo.simularSinRed(false)
        esperar { puts.get() >= 1 }
        repo.actualizar(creado.id, datos("Última edición local"))
        esperar { db.recordatorios().buscar(creado.id)?.sincronizacion == "Sincronizado" }
        assertEquals("Última edición local", repo.obtenerPorId(creado.id)?.titulo)
        assertEquals("Última edición local", remotos.values.single().titulo)
        assertEquals(1, repo.obtener().size)
    }
    @Test fun eliminacionOfflineSeConfirmaAlRecuperarConexion() = runBlocking {
        val creado = repo.guardar(datos()); repo.simularSinRed(false)
        esperar { db.recordatorios().buscar(creado.id)?.sincronizacion == "Sincronizado" }
        repo.simularSinRed(true); repo.eliminar(creado.id)
        assertEquals(0, repo.obtener().size)
        repo.simularSinRed(false)
        esperar { db.recordatorios().buscar(creado.id) == null }
        assertTrue(remotos.isEmpty())
    }
}
