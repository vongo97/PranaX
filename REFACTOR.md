# Documentación de Refactorización y Arquitectura (PranaX / BreathingApp)

Este documento detalla las mejoras de arquitectura, seguridad local y diseño visual implementadas en el proyecto para alinearlo con las normas de **Clean Code** y **Clean Architecture**.

---

## 1. Arquitectura de UI Limpia (MVVM y Desacoplamiento)

Se eliminaron los acoplamientos directos entre las vistas (Jetpack Compose Screens) y los repositorios de persistencia o servicios de red. El flujo de datos ahora está 100% estructurado bajo el patrón MVVM:

* **Centralización de Estados**: Las pantallas observan el estado de la configuración a través de un flujo caliente `StateFlow<AppSettings>` expuesto por `SettingsViewModel`.
* **Remoción de Corrutinas en UI**: Las mutaciones de estado y escrituras locales (ej. habilitar vibración, campana, meditación guiada) se delegan al ViewModel, eliminando bloques `coroutineScope.launch` innecesarios de las pantallas.
* **Refactorización de Pantallas**:
  * `SessionPrepScreen.kt`: Ahora inyecta y consume `SettingsViewModel`.
  * `BreathingScreen.kt`: Ahora inyecta y consume `SettingsViewModel` para leer las preferencias y registrar la finalización de los ejercicios.
  * `SettingsScreen.kt` y `ProfileScreen.kt`: Adaptadas para consumir sus respectivos ViewModels (`SettingsViewModel` y `ProfileViewModel`).

---

## 2. Seguridad en Hashing de Credenciales (Neon Sync)

Para mitigar riesgos de seguridad locales y proteger las credenciales del usuario:

* **Hashing Seguro con Sal (Salt)**: La función de hash de contraseñas en `NeonSyncRepository` fue modificada para utilizar el algoritmo **SHA-256 junto con una sal única dinámica** (basada en el correo del usuario). Esto previene ataques de fuerza bruta y de diccionario (tablas rainbow).
* **Migración Transparente**: Se implementó un mecanismo de migración automática durante el inicio de sesión. Si el sistema detecta que el usuario tiene un hash antiguo (sin sal), realiza la verificación con el método anterior, genera automáticamente el nuevo hash salado y actualiza la base de datos de Neon SQL sin alterar la experiencia del usuario.

---

## 3. Control Asíncrono de Alarmas y Ciclo de Vida

* **ReminderReceiver.kt**: Para evitar que el sistema operativo Android finalice prematuramente el proceso al recibir la difusión en segundo plano (`BOOT_COMPLETED` o reprogramación de alarmas), se implementó la API `goAsync()`.
* La reprogramación de alarmas ahora ocurre de manera segura en un hilo de fondo de Kotlin Coroutines (`Dispatchers.IO`), asegurando que finalice correctamente llamando a `pendingResult.finish()` en un bloque `finally`.

---

## 4. Diseño Visual Edge-to-Edge y Barra de Estado

Para asegurar una visualización inmersiva en la que el fondo de la app cubra el 100% de la pantalla física sin franjas grises (especialmente en dispositivos con capas personalizadas como MagicOS de Honor):

* **MainActivity.kt**:
  * Se configuró el inicio de la app mediante la API oficial `enableEdgeToEdge(...)` de AndroidX Activity.
  * Se aplicó `FLAG_LAYOUT_NO_LIMITS` a la ventana de la Activity para eliminar límites físicos y forzar a que el fondo se dibuje debajo de la barra de estado.
  * Se integró `WindowInsetsControllerCompat` para obligar al sistema a pintar los iconos superiores (hora, wifi, batería) en **color blanco/claro** sobre el fondo de bosque/noche de la app.
* **Temas XML**:
  * Los archivos `themes.xml` (tanto en la carpeta de recursos comunes como en `values-v29`) fueron modificados para heredar de `android:Theme.Material.NoActionBar`. Esto bloquea cualquier color impuesto por el tema del sistema del fabricante (`Theme.DeviceDefault`) y garantiza una transparencia real y controlada.
