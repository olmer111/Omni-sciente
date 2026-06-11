# Omni-sciente — Asistente de automatización legítimo

Herramienta de automatización personal por voz para Android, con un modelo de
confianza centrado en el usuario: tú defines cada macro, son visibles y editables,
y el asistente **se aparta de pantallas sensibles** (banca, contraseñas, pagos)
en vez de leerlas.

## Qué incluye

- **Pipeline de voz local** (Vosk, offline) con dos modos: comandos (gramática
  restringida, alta precisión) y dictado (vocabulario libre).
- **Servicio de accesibilidad** para navegación, toques y escritura asistida.
- **Editor de macros** donde creas, ordenas y editas pasos a mano. Conjunto
  cerrado de acciones (esperar, tocar por texto, escribir, navegar). Sin
  importación ni ejecución de scripts externos.
- **GuardiaContexto**: cortafuegos que niega la automatización sobre apps de
  banca/pagos y se detiene si detecta campos de contraseña. Validado por tests.
- **Frenos de emergencia**: agitar el teléfono o mantener bajar-volumen aborta
  cualquier ejecución en curso.
- **Burbuja de estado** flotante (escuchando / dictando / sin voz).

## Requisitos

- Android Studio (Ladybug o superior) **o** SDK de línea de comandos.
- Android SDK Platform 34, Build-Tools 34.
- JDK 17.
- Un dispositivo/emulador con Android 8.0 (API 26) o superior.

## Paso 1 — Colocar el modelo de voz

El reconocimiento offline necesita el modelo de Vosk en español (no se incluye
por tamaño/licencia):

1. Descarga `vosk-model-small-es-0.42` (~40 MB) de https://alphacephei.com/vosk/models
2. Descomprímelo y renombra la carpeta a `model-es`
3. Colócala en `app/src/main/assets/model-es/`

Ver `app/src/main/assets/COLOCAR_MODELO_AQUI.txt`. Sin el modelo, la app compila
y arranca, pero el reconocimiento queda en estado "sin voz".

## Paso 2 — Compilar el APK

### Opción A: Android Studio
1. *File > Open* y selecciona esta carpeta.
2. Deja que sincronice Gradle.
3. *Build > Build Bundle(s) / APK(s) > Build APK(s)*.
4. El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Opción B: línea de comandos
Necesitas el Gradle wrapper. Si no está el `gradlew`, genéralo una vez con
`gradle wrapper` (teniendo Gradle 8.9 instalado), o usa tu Gradle local:

```bash
gradle assembleDebug      # o:  ./gradlew assembleDebug
```

## Paso 3 — Correr los tests de seguridad

```bash
gradle test               # o:  ./gradlew test
```

Verás los tests de `GuardiaContexto` (apps bloqueadas, negar por defecto) y de
`pantallaSensible` (detección de campos de contraseña).

## Paso 4 — Instalar en tu teléfono

Con depuración USB activada:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Al abrir, concede los permisos desde la pantalla de onboarding (accesibilidad,
micrófono, superposición) y pulsa *Iniciar asistente*.

## Para producción

El APK debe firmarse con **tu** clave (`keytool` + configuración de `signingConfig`),
no con la de depuración. Y como usa `BIND_ACCESSIBILITY_SERVICE`, Play Store exige
una política de privacidad y una declaración de uso del permiso de accesibilidad.
El diseño de esta app (sin lectura de pantallas sensibles, sin red, todo local)
facilita esa declaración.

## Estructura

```
app/src/main/java/com/omnisciente/
├── service/      OmniAccessibilityService, OmniForegroundService
├── audio/        AudioCommandReceiver, VozManager, Vosk..., TranscriptorLocal
├── core/         OmniOrchestrator, DictadoController
├── overlay/      OverlayBurbuja
├── safety/       GuardiaContexto, ParadaEmergencia, DetectorAgitacion
├── macro/        Macro, MacroRepositorio, EjecutorMacro, editor + diálogo
└── setup/        OnboardingActivity, PermisosHelper
```
