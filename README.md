# Recordatorios contextuales

Aplicación de Programación Mobile desarrollada con Kotlin, Jetpack Compose y Material 3.
La versión 2 continúa la Actividad 1 e incorpora navegación, formularios y arquitectura de presentación.

## Abrir y ejecutar
Abrir esta carpeta con Android Studio y sincronizar Gradle. Usar el JDK integrado de Android Studio y tener instalado Android SDK 37. Seleccionar un emulador Android API 28 o superior y ejecutar la configuración app.

## Organización
- `Models.kt`: modelos inmutables y validaciones; `ReminderData.kt`: ejemplos y colecciones de la primera actividad.
- `data/RecordatoriosRepository.kt`: contrato de datos e implementación en memoria compartida durante la sesión.
- `presentation/UiState.kt`: estados tipados, filtros y validación del formulario.
- `presentation/RecordatoriosViewModels.kt`: ViewModel de listado, creación y detalle; StateFlow de solo lectura y tareas en viewModelScope.
- `ui/Componentes.kt`: tarjeta, campos controlados, selectores y mensajes de estado reutilizables.
- `ui/Pantallas.kt`: listado, creación y detalle, con Material 3 e innerPadding de Scaffold.
- `ui/RecordatoriosNavHost.kt`: rutas `listado`, `crear` y `detalle/{id}`. Solo se transporta un identificador entero.
- `RecordatoriosApplication.kt`: instancia compartida del repositorio. Los ViewModel solo reciben su interfaz.

## Usar la versión 2

El botón `+` abre el formulario. El título requiere 4 caracteres después de trim;
la descripción admite hasta 200; categoría y prioridad son obligatorias. Guardar se
deshabilita si el formulario es inválido o está enviando. Cancelar descarta la operación,
incluso durante la demora simulada. Cada tarjeta abre el detalle. Guardar y cancelar
usan popBackStack para volver sin duplicar el listado.

`Simular estados` muestra los botones Datos, Carga, Vacío y Error. La carga dura
aproximadamente 2,8 segundos y vuelve a los datos. Vacío no elimina los registros;
Datos restaura la vista normal. Error muestra Reintentar, que vuelve a cargar los datos.
El formulario permite activar `Simular error de guardado`; desactivarlo permite reintentar
sin perder los campos. La creación normal tarda aproximadamente 1,4 segundos.

Los ViewModel se conservan ante la recreación de Activity y la interfaz observa sus
flujos con collectAsStateWithLifecycle. El repositorio vive solo en memoria: al finalizar
el proceso se recuperan los cinco ejemplos, sin conservar los registros creados.

## Capturas para la Actividad 2

1. Listado normal con título, categoría, prioridad y estado.
2. Formulario: `+`, completar título y descripción y seleccionar categoría y prioridad.
3. Detalle: seleccionar `Turno médico` para mostrar también los campos opcionales.
4. Vacío: `Simular estados` → `Vacío`.
5. Error y reintento: `Simular estados` → `Error`.
6. Estructura: expandir app/src/main, data, presentation, ui y Gradle Scripts en Android Studio.

Las simulaciones son herramientas de comprobación, no fallos de red reales.

## Comprobaciones
`gradlew.bat assembleDebug testDebugUnitTest`

Con un emulador abierto: `gradlew.bat connectedDebugAndroidTest`.

Los datos son simulados. No se solicitan permisos ni se implementan persistencia, nube o notificaciones en esta segunda actividad.
