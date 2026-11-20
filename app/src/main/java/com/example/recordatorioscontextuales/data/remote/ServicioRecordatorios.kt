package com.example.recordatorioscontextuales.data.remote

import com.example.recordatorioscontextuales.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class RecordatorioDto(
    val id: String, val titulo: String, val descripcion: String, val categoria: String,
    val prioridad: String, val estado: String, val fecha: String, val revision: Long,
    val vencimiento: String? = null, val latitud: Double? = null, val longitud: Double? = null,
    val lugar: String? = null, val aviso: String? = null
)
interface ServicioRecordatorios {
    @GET("recordatorios") suspend fun listar(): List<RecordatorioDto>
    // El mismo UUID y versión siempre reemplazan el mismo recurso, nunca crean otro.
    @PUT("recordatorios/{id}") suspend fun guardar(@Path("id") id: String, @Body registro: RecordatorioDto): RecordatorioDto
    @DELETE("recordatorios/{id}") suspend fun eliminar(@Path("id") id: String)
}
object ServicioFactory {
    fun crear(url: String = BuildConfig.API_BASE_URL, timeout: Long = 3): ServicioRecordatorios = Retrofit.Builder()
        .baseUrl(url).client(OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS).writeTimeout(timeout, TimeUnit.SECONDS)
            .callTimeout(timeout + 2, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ServicioRecordatorios::class.java)
}
