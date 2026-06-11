# Omni-sciente

Asistente de automatización personal por voz para Android. Creas macros con tu voz, las editas a mano, y el asistente las ejecuta — sin tocar apps de banca, contraseñas ni pagos.

---

## Instalar la app (sin necesitar Android Studio)

### Paso 1 — Descargar el APK

1. Ve a la pestaña **[Actions](../../actions)** de este repositorio en GitHub
2. Abre el workflow más reciente llamado **Build APK** (debe tener un ✓ verde)
3. Baja hasta la sección **Artifacts** y descarga **Omnisciente-debug**
4. Descomprime el `.zip` descargado — dentro está el archivo `app-debug.apk`

> Si el workflow todavía está corriendo (círculo amarillo), espera ~5 minutos.

---

### Paso 2 — Permitir instalación de apps externas

En tu teléfono Android:

1. Ve a **Ajustes**
2. Busca **Seguridad** (o "Privacidad y seguridad")
3. Activa **Fuentes desconocidas** — o, en Android 8+, cuando abras el APK te pedirá permiso directamente para el navegador/gestor de archivos que uses

---

### Paso 3 — Instalar el APK

1. Abre el archivo `app-debug.apk` desde tu teléfono (en la carpeta de Descargas)
2. Toca **Instalar**
3. Espera unos segundos — listo

---

### Paso 4 — Configurar la app al abrirla

Al abrir Omni-sciente por primera vez verás la pantalla de bienvenida. Te pedirá 3 permisos:

| Permiso | Para qué sirve |
|---|---|
| **Accesibilidad** | Ejecutar los pasos de tus macros (tocar, escribir, navegar) |
| **Micrófono** | Escuchar tus comandos de voz |
| **Superposición** | Mostrar la burbuja flotante de estado |

Concede los tres y pulsa **Iniciar asistente**.

---

### Paso 5 — (Opcional) Activar reconocimiento de voz

La app incluye reconocimiento de voz **offline** con Vosk. Para activarlo:

1. Descarga el modelo en español (~40 MB): [vosk-model-small-es-0.42](https://alphacephei.com/vosk/models)
2. Descomprime y renombra la carpeta a `model-es`
3. Copia la carpeta a `Android/data/com.omnisciente/files/model-es/` en tu teléfono

> Sin el modelo, la burbuja aparece en **rojo** (sin voz). Puedes seguir creando y ejecutando macros manualmente desde el editor.

---

## Qué puede hacer la app

**Voz y comandos**
- **Palabra de activación** — di *"oye asistente"* para despertarlo sin tocar nada; tras 20 s en silencio se vuelve a dormir solo (burbuja lila)
- **Skills integradas, todo offline** — temporizadores, alarmas, calculadora hablada (*"cuánto es treinta y cinco por dos"*), fecha y hora, y abrir apps por nombre (*"abre el navegador"*)
- **Dictado por voz** a cualquier campo de texto
- **Ejecutar macros por voz** — *"ejecuta enviar reporte"*

**Macros**
- **Editor visual** con acciones: tocar por texto, tocar coordenada, escribir, deslizar (4 direcciones), abrir app, control de medios (play/pausa/siguiente/anterior), ajustar volumen, navegar y esperar
- **Grabador de macros** — graba tus toques y escritura reales y los convierte en pasos editables (no graba en pantallas de banca ni contraseñas)

**Seguridad**
- **Protección automática** — se detiene sola en apps de banca, pagos y campos de contraseña
- **Frenos de emergencia** — agita el teléfono o mantén el botón de bajar volumen para abortar
- **Sin nube** — todo el reconocimiento y procesamiento ocurre en el dispositivo

---

## Compilar tú mismo (opcional)

Si quieres modificar el código:

**Requisitos:** Android Studio Ladybug+, JDK 17, Android SDK 34

```bash
git clone https://github.com/olmer111/Omni-sciente.git
cd Omni-sciente
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Estructura del código

```
app/src/main/java/com/omnisciente/
├── service/    Servicios de accesibilidad y foreground
├── audio/      Pipeline de voz (Vosk, transcripción)
├── core/       Orquestador y controlador de dictado
├── overlay/    Burbuja flotante de estado
├── safety/     GuardiaContexto, frenos de emergencia
├── macro/      Editor y ejecutor de macros
└── setup/      Onboarding y permisos
```
