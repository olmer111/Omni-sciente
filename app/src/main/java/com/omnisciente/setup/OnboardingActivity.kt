package com.omnisciente.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omnisciente.macro.MacroEditorActivity
import com.omnisciente.service.OmniForegroundService

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { OnboardingScreen() } }
    }
}

@Composable
private fun OnboardingScreen() {
    val context = LocalContext.current

    var accesibilidad by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    var microfono by remember { mutableStateOf(false) }
    var servicioActivo by remember { mutableStateOf(false) }

    fun refrescar() {
        accesibilidad = PermisosHelper.accesibilidadActiva(context)
        overlay = PermisosHelper.overlayActivo(context)
        microfono = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refrescar()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val pedirMicrofono = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> microfono = concedido }

    val pedirNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val todoListo = accesibilidad && overlay && microfono

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configura Omni-sciente", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Concede estos permisos para que el asistente pueda leer la pantalla, " +
                "guiarte con superposiciones y escucharte. Tu mandas: nada se ejecuta sin que lo pidas.",
            style = MaterialTheme.typography.bodyMedium
        )

        TarjetaPermiso(
            "Servicio de accesibilidad",
            "Permite leer la pantalla y ejecutar tus comandos.",
            accesibilidad
        ) { PermisosHelper.abrirAjustesAccesibilidad(context) }

        TarjetaPermiso(
            "Microfono",
            "Para escuchar tus comandos de voz, en el dispositivo.",
            microfono
        ) { pedirMicrofono.launch(Manifest.permission.RECORD_AUDIO) }

        TarjetaPermiso(
            "Superposicion en pantalla",
            "Para mostrar guias flotantes sobre otras apps.",
            overlay
        ) { PermisosHelper.abrirAjustesOverlay(context) }

        TarjetaPermiso(
            "Bateria sin restricciones (opcional)",
            "Evita que el sistema cierre el asistente en segundo plano.",
            concedido = false,
            mostrarCheck = false
        ) { PermisosHelper.abrirExclusionBateria(context) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !servicioActivo) {
                    pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                servicioActivo = alternarServicio(context, encender = !servicioActivo)
            },
            enabled = todoListo,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (servicioActivo) "Detener asistente" else "Iniciar asistente") }

        Button(
            onClick = { context.startActivity(Intent(context, MacroEditorActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Abrir mis macros") }

        if (!todoListo) {
            Text(
                "Concede los tres permisos de arriba para activar el asistente.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun TarjetaPermiso(
    titulo: String,
    descripcion: String,
    concedido: Boolean,
    mostrarCheck: Boolean = true,
    onAccion: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(titulo, fontWeight = FontWeight.SemiBold)
                Text(descripcion, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            if (mostrarCheck && concedido) {
                Text("\u2713 Listo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = onAccion) { Text(if (mostrarCheck) "Conceder" else "Abrir") }
            }
        }
    }
}

private fun alternarServicio(context: Context, encender: Boolean): Boolean {
    val intent = Intent(context, OmniForegroundService::class.java)
    if (encender) ContextCompat.startForegroundService(context, intent)
    else context.stopService(intent)
    return encender
}
