package com.example.recordatorioscontextuales

import android.app.Application
import com.example.recordatorioscontextuales.data.InMemoryRecordatoriosRepository
import com.example.recordatorioscontextuales.data.RecordatoriosRepository

class RecordatoriosApplication : Application() {
    // Una fuente compartida por las pantallas, conservada durante la sesión del proceso.
    val repository: RecordatoriosRepository by lazy { InMemoryRecordatoriosRepository() }
}
