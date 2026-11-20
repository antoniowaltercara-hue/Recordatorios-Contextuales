# Recordatorios contextuales

Aplicación de Antonio Walter Cara para Programación Mobile. La versión 3.0 continúa las actividades 1 y 2 en el mismo proyecto y repositorio.

## Abrir, compilar e instalar

1. Abrir la carpeta raíz del proyecto en Android Studio y sincronizar Gradle.
2. Usar el JDK integrado, Android SDK 37 y un emulador o teléfono con API 28 o superior.
3. Ejecutar la configuración `app`. Se crea una base Room y se insertan los cinco ejemplos solo la primera vez.
4. Para compilar desde Windows: `gradlew.bat assembleDebug`.
5. El APK de prueba queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Iniciar el servicio simulado

Se necesita Python 3, sin paquetes adicionales. Desde la raíz del proyecto:

```cmd
py tools/servidor_demo.py
```

También se puede abrir `tools/iniciar_servidor.bat`. Mantener la terminal del servidor abierta. El servicio guarda sus registros en `tools/datos_servidor.json`, excluido de Git.

El lanzador configura `adb reverse tcp:8765 tcp:8765` con el emulador o teléfono USB conectado. Iniciar el dispositivo antes que el servidor. La URL predeterminada es `http://127.0.0.1:8765/`. El ejemplo `gradle.properties.example` muestra cómo configurar `API_BASE_URL`. Agregar esa propiedad a `gradle.properties`, sincronizar y compilar. La URL debe terminar con `/`.

Un teléfono USB puede usar el mismo túnel ADB. Como alternativa sin USB, configurar la IP local de la PC y ejecutar el servidor con `--host 0.0.0.0`; requiere conectividad local y permiso de acceso al puerto. En el emulador también puede configurarse la dirección habitual 10.0.2.2 si la red del equipo lo permite. No hay usuarios, contraseñas ni tokens. HTTP está permitido solo en debug; una distribución real requiere HTTPS.

## Funciones de la versión 3

- Listado, creación, detalle, edición y eliminación. Se mantienen los modelos y las validaciones anteriores.
- Las tres preferencias se abren desde **Ajustes**: tema oscuro, mostrar completados y orden por fecha, título o prioridad.
- Room conserva los registros al cerrar el proceso. DataStore guarda las preferencias. Solo borrar los datos de la app o desinstalar elimina esos archivos.
- Cada tarjeta y detalle muestra Pendiente, Sincronizado o Error.
- **Simular sin conexión** permite probar el trabajo local sin depender del servicio. No borra registros y se desactiva al iniciar otra sesión.
- **Sincronizar ahora** vuelve a intentar el envío. También se intenta al iniciar el proceso y cuando Android informa que hay red. No se utiliza WorkManager: con el proceso cerrado, los cambios esperan a la siguiente apertura.
- La eliminación se guarda como una marca local hasta que el servicio la confirma. Se oculta inmediatamente del listado.

## Capas y sincronización

`ui` presenta pantallas; `presentation` mantiene los estados y eventos. `data/local` contiene entidad, DAO, base Room y DataStore; `data/remote` contiene DTO, Retrofit y OkHttp. `Mapeos.kt` transforma entidad, DTO y dominio. `LocalFirstRepository` implementa el contrato que reciben los ViewModel.

El flujo principal es Compose → ViewModel → repositorio → Room → Flow → pantalla. El repositorio complementa Room con Retrofit → servicio → DTO validado → Room. La interfaz no lee directamente de la red.

Cada registro tiene un ID local y un UUID remoto único. Se usa PUT sobre ese UUID, no POST en cada reintento. Un Mutex impide sincronizaciones simultáneas. El número de revisión evita confirmar una edición con una respuesta anterior. Los DTO inválidos se rechazan antes de escribirlos; el índice único evita duplicados importados.

La demo contempla un único usuario/dispositivo. Un conflicto HTTP 409 conserva el cambio local y requiere resolver el dato antes de reintentar; no se implementa una combinación automática de cambios de varios dispositivos.

## Comprobar errores del servicio

Abrir en el navegador de la PC:

- `http://127.0.0.1:8765/control?modo=500`: error del servidor.
- `http://127.0.0.1:8765/control?modo=400`: solicitud inválida.
- `http://127.0.0.1:8765/control?modo=timeout`: demora de 6 segundos, mayor al timeout de lectura del cliente.
- `http://127.0.0.1:8765/control?modo=normal`: restablecer el servicio normal.
- `http://127.0.0.1:8765/estado`: cantidad de registros remotos y modo actual.
- `http://127.0.0.1:8765/recordatorios`: registros recibidos por el servicio.

Después de cambiar el modo, tocar **Sincronizar ahora** en Ajustes. Un fallo conserva la información local y ofrece reintento. Repetir la sincronización normal no aumenta la cantidad de registros.

## Preferencias predeterminadas y lectura inválida

Sin preferencias guardadas se aplica tema claro, completados visibles y orden por fecha. Un valor de orden desconocido o una clave con un tipo incorrecto usa su valor predeterminado. Un IOException de lectura produce preferencias predeterminadas; la corrupción del archivo usa ReplaceFileCorruptionHandler. No se guardan recordatorios ni secretos en DataStore.

## Pruebas

```cmd
gradlew.bat testDebugUnitTest
gradlew.bat connectedDebugAndroidTest
py -m unittest discover -s tools -p test_servidor.py
```

Las pruebas locales cubren validaciones y mapeos. Las instrumentadas prueban Room, DataStore, CRUD, reapertura, filtros, timeout, HTTP 400 y 500, DTO inválido, envío concurrente, edición durante el envío, eliminación pendiente y las pantallas de la versión anterior.

## Capturas para el informe

El Word y el PDF de `entregas/actividad3` dejan zonas numeradas y pasos para obtener cada evidencia. Completar las imágenes en Word y exportar nuevamente el PDF antes de entregar. Ambos informes tienen un enlace clickeable al repositorio. La consigna también solicita un ZIP del proyecto.

## Alcance

No se implementan mapas, notificaciones, autenticación real, cifrado adicional ni sincronización de fondo con el proceso detenido. La persistencia local y la sincronización descritas sí pertenecen a esta versión. La clase InMemoryRecordatoriosRepository se conserva para las pruebas históricas de la actividad 2; la aplicación utiliza LocalFirstRepository y Room.
