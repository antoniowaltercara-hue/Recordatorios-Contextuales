package com.example.recordatorioscontextuales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import com.example.recordatorioscontextuales.ui.RecordatoriosNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as RecordatoriosApplication).repository
        setContent {
            val preferencias by repository.preferencias.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = if (preferencias.oscuro) darkColorScheme() else lightColorScheme(
                primary = Color(0xFF285C55), background = Color(0xFFF5F7F6), surface = Color.White
            )) { RecordatoriosNavHost(repository) }
        }
    }
}
