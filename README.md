# Recordatorios contextuales

Actividad 1 de Programación Mobile. Kotlin, Jetpack Compose y Material 3.

## Abrir y ejecutar
Abrir esta carpeta con Android Studio y sincronizar Gradle. Usar el JDK integrado de Android Studio y tener instalado Android SDK 37. Seleccionar un emulador Android API 28 o superior y ejecutar la configuración app.

## Organización
- Models.kt: modelos inmutables y validaciones.
- ReminderData.kt: cinco ejemplos, List, Set, Map y filtros.
- MainActivity.kt: lista Compose y filtros Todos, Pendientes y Alta prioridad.

## Comprobaciones
`gradlew.bat assembleDebug testDebugUnitTest`

Con un emulador abierto: `gradlew.bat connectedDebugAndroidTest`.

Los datos son simulados. No se solicitan permisos ni se implementan persistencia, nube o notificaciones en esta primera actividad.
