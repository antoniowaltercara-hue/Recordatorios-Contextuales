package com.example.recordatorioscontextuales.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.recordatorioscontextuales.ReminderData
import com.example.recordatorioscontextuales.data.aEntidad

@Entity(tableName = "recordatorios", indices = [Index(value = ["remotoId"], unique = true)])
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remotoId: String,
    val titulo: String, val descripcion: String, val categoria: String,
    val prioridad: String, val estado: String, val fecha: String,
    val vencimiento: String? = null, val latitud: Double? = null,
    val longitud: Double? = null, val lugar: String? = null, val aviso: String? = null,
    val revision: Long = 1,
    val sincronizacion: String = "Pendiente",
    val eliminado: Boolean = false
)

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 ORDER BY id")
    fun observar(): Flow<List<RecordatorioEntity>>
    @Query("SELECT * FROM recordatorios WHERE id = :id AND eliminado = 0")
    fun observarPorId(id: Int): Flow<RecordatorioEntity?>
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 AND estado = :estado ORDER BY id")
    fun filtrarPorEstado(estado: String): Flow<List<RecordatorioEntity>>
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 ORDER BY id")
    suspend fun listar(): List<RecordatorioEntity>
    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun buscar(id: Int): RecordatorioEntity?
    @Query("SELECT * FROM recordatorios WHERE remotoId = :id")
    suspend fun buscarRemoto(id: String): RecordatorioEntity?
    @Query("SELECT * FROM recordatorios WHERE sincronizacion != 'Sincronizado' ORDER BY id")
    suspend fun pendientes(): List<RecordatorioEntity>
    @Insert suspend fun insertar(registro: RecordatorioEntity): Long
    @Update suspend fun actualizar(registro: RecordatorioEntity)
    @Query("UPDATE recordatorios SET eliminado = 1, revision = revision + 1, sincronizacion = 'Pendiente' WHERE id = :id")
    suspend fun marcarEliminado(id: Int)
    @Query("UPDATE recordatorios SET sincronizacion = :estado WHERE id = :id AND revision = :revision")
    suspend fun confirmar(id: Int, revision: Long, estado: String)
    @Query("DELETE FROM recordatorios WHERE id = :id AND revision = :revision AND eliminado = 1")
    suspend fun borrarConfirmado(id: Int, revision: Long)
}

@Database(entities = [RecordatorioEntity::class], version = 1, exportSchema = true)
abstract class BaseDatos : RoomDatabase() {
    abstract fun recordatorios(): RecordatorioDao
    companion object {
        @Volatile private var instancia: BaseDatos? = null
        fun obtener(context: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, BaseDatos::class.java,
                "recordatorios.db").addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Solo en la creación inicial de la base: nunca repoblar una lista vaciada.
                        ReminderData.recordatorios.forEach { r ->
                            val e = r.aEntidad()
                            db.execSQL("INSERT INTO recordatorios (id,remotoId,titulo,descripcion,categoria,prioridad,estado,fecha,vencimiento,latitud,longitud,lugar,aviso,revision,sincronizacion,eliminado) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                arrayOf<Any?>(e.id,e.remotoId,e.titulo,e.descripcion,e.categoria,e.prioridad,e.estado,e.fecha,e.vencimiento,e.latitud,e.longitud,e.lugar,e.aviso,e.revision,e.sincronizacion,0))
                        }
                    }
                }).build().also { instancia = it }
        }
    }
}
