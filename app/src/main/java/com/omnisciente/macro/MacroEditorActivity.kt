package com.omnisciente.macro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnisciente.audio.VozManager
import com.omnisciente.safety.GuardiaContexto
import com.omnisciente.service.OmniAccessibilityService
import kotlinx.coroutines.launch
import java.util.UUID

class MacroEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PantallaMacros() } }
    }
}

@Composable
private fun PantallaMacros() {
    val context = LocalContext.current
    val repo = remember { MacroRepositorio(context) }
    var macros by remember { mutableStateOf(repo.cargarTodas()) }
    var editando by remember { mutableStateOf<Macro?>(null) }

    val scope = rememberCoroutineScope()
    val voz = remember { VozManager(context) }
    val ejecutor = remember {
        EjecutorMacro(voz = voz, servicio = { OmniAccessibilityService.instance })
    }
    var estadoEjecucion by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { voz.apagar() } }

    if (editando != null) {
        EditorDeUnaMacro(
            macro = editando!!,
            onGuardar = { actualizada ->
                repo.guardar(actualizada)
                macros = repo.cargarTodas()
                editando = null
            },
            onCancelar = { editando = null }
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Mis macros", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tu creas cada macro y eliges sobre que app actua. Se detienen solas " +
                "ante pantallas de banca o contrasenas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        estadoEjecucion?.let { msg ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(msg, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Button(onClick = {
            editando = Macro(UUID.randomUUID().toString(), "Nueva macro", "", emptyList())
        }) { Text("Crear macro") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(macros, key = { it.id }) { macro ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(macro.nombre, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${macro.pasos.size} pasos - ${macro.appObjetivo.ifEmpty { "sin app" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        TextButton(onClick = {
                            val svc = OmniAccessibilityService.instance
                            if (svc == null) {
                                estadoEjecucion = "Activa el servicio de accesibilidad primero."
                                return@TextButton
                            }
                            if (macro.pasos.isEmpty()) {
                                estadoEjecucion = "Esta macro no tiene pasos."
                                return@TextButton
                            }
                            estadoEjecucion = "Ejecutando ${macro.nombre}..."
                            scope.launch {
                                val resultado = ejecutor.ejecutar(macro)
                                estadoEjecucion = when (resultado) {
                                    is EjecutorMacro.Resultado.Completada ->
                                        "${macro.nombre}: completada."
                                    is EjecutorMacro.Resultado.Abortada ->
                                        "${macro.nombre}: detenida - ${resultado.motivo}."
                                }
                            }
                        }) { Text("Ejecutar") }
                        TextButton(onClick = { editando = macro }) { Text("Editar") }
                        TextButton(onClick = {
                            repo.eliminar(macro.id); macros = repo.cargarTodas()
                        }) { Text("Borrar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorDeUnaMacro(
    macro: Macro,
    onGuardar: (Macro) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(macro.nombre) }
    var appObjetivo by remember { mutableStateOf(macro.appObjetivo) }
    var pasos by remember { mutableStateOf(macro.pasos) }
    var error by remember { mutableStateOf<String?>(null) }
    var pasoEnEdicion by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Editar macro", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = appObjetivo, onValueChange = { appObjetivo = it; error = null },
            label = { Text("Paquete de la app (ej. com.miapp.notas)") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        Text("Pasos", fontWeight = FontWeight.SemiBold)

        pasos.forEachIndexed { i, paso ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { pasoEnEdicion = i }, modifier = Modifier.weight(1f)) {
                    Text("${i + 1}. ${describirPaso(paso)}", modifier = Modifier.fillMaxWidth())
                }
                TextButton(enabled = i > 0, onClick = { pasos = mover(pasos, i, i - 1) }) { Text("\u2191") }
                TextButton(enabled = i < pasos.size - 1, onClick = { pasos = mover(pasos, i, i + 1) }) { Text("\u2193") }
                TextButton(onClick = { pasos = pasos.filterIndexed { idx, _ -> idx != i } }) { Text("\u2715") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {
                pasos = pasos + PasoMacro.Esperar(500); pasoEnEdicion = pasos.size - 1
            }, label = { Text("+ Esperar") })
            AssistChip(onClick = {
                pasos = pasos + PasoMacro.TocarPorTexto(""); pasoEnEdicion = pasos.size - 1
            }, label = { Text("+ Tocar") })
            AssistChip(onClick = {
                pasos = pasos + PasoMacro.EscribirTexto(""); pasoEnEdicion = pasos.size - 1
            }, label = { Text("+ Escribir") })
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(modifier = Modifier.weight(1f), onClick = {
                if (!GuardiaContexto.appPermitida(appObjetivo)) {
                    error = "Esa app no se permite para automatizacion por seguridad."
                    return@Button
                }
                onGuardar(macro.copy(nombre = nombre, appObjetivo = appObjetivo, pasos = pasos))
            }) { Text("Guardar") }
        }
    }

    pasoEnEdicion?.let { idx ->
        pasos.getOrNull(idx)?.let { paso ->
            DialogoEditarPaso(
                paso = paso,
                onConfirmar = { actualizado ->
                    pasos = pasos.toMutableList().also { it[idx] = actualizado }
                    pasoEnEdicion = null
                },
                onCancelar = {
                    val p = pasos.getOrNull(idx)
                    val vacio = when (p) {
                        is PasoMacro.TocarPorTexto -> p.texto.isEmpty()
                        is PasoMacro.EscribirTexto -> p.contenido.isEmpty()
                        else -> false
                    }
                    if (vacio) pasos = pasos.filterIndexed { i, _ -> i != idx }
                    pasoEnEdicion = null
                }
            )
        }
    }
}

private fun describirPaso(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Esperar -> "Esperar ${paso.ms} ms"
    is PasoMacro.TocarPorTexto -> "Tocar \"${paso.texto}\""
    is PasoMacro.EscribirTexto -> "Escribir \"${paso.contenido}\""
    is PasoMacro.Navegar -> "Navegar (${paso.accionGlobal})"
}

private fun <T> mover(lista: List<T>, desde: Int, hacia: Int): List<T> {
    val mut = lista.toMutableList()
    mut.add(hacia, mut.removeAt(desde))
    return mut
}
