package com.omnisciente.macro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Dialogo para editar el contenido de un paso individual. */
@Composable
fun DialogoEditarPaso(
    paso: PasoMacro,
    onConfirmar: (PasoMacro) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(textoInicial(paso)) }
    var texto2 by remember { mutableStateOf(textoSecundarioInicial(paso)) }
    var opcion by remember { mutableStateOf(opcionInicial(paso)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(tituloPaso(paso)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (paso) {
                    is PasoMacro.Esperar -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it.filter(Char::isDigit); error = null },
                            label = { Text("Milisegundos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Nota("Pausa antes del siguiente paso.")
                    }
                    is PasoMacro.TocarPorTexto -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it; error = null },
                            label = { Text("Texto del boton o elemento") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Nota("La macro buscara un elemento con este texto y lo tocara.")
                    }
                    is PasoMacro.EscribirTexto -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it; error = null },
                            label = { Text("Texto a escribir") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Nota("Se escribe en el campo que este enfocado en ese momento.")
                    }
                    is PasoMacro.Navegar -> {
                        Text("Paso de navegacion; no requiere edicion de texto.")
                    }
                    is PasoMacro.TocarCoordenada -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = texto,
                                onValueChange = { texto = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                                label = { Text("X (px)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = texto2,
                                onValueChange = { texto2 = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                                label = { Text("Y (px)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Nota("Toque en una posicion exacta de la pantalla, en pixeles.")
                    }
                    is PasoMacro.Deslizar -> {
                        Nota("Direccion del deslizamiento:")
                        SelectorOpciones(
                            opciones = PasoMacro.Direccion.entries.map { it.name to etiquetaDireccion(it) },
                            seleccionada = opcion,
                            onSeleccion = { opcion = it; error = null }
                        )
                    }
                    is PasoMacro.AbrirApp -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it; error = null },
                            label = { Text("Paquete de la app (ej. com.miapp.notas)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Nota("Abre la app indicada. Las apps de banca y pagos estan bloqueadas.")
                    }
                    is PasoMacro.ControlMedios -> {
                        Nota("Accion multimedia:")
                        SelectorOpciones(
                            opciones = PasoMacro.AccionMedios.entries.map { it.name to etiquetaMedios(it) },
                            seleccionada = opcion,
                            onSeleccion = { opcion = it; error = null }
                        )
                    }
                    is PasoMacro.AjustarVolumen -> {
                        Nota("Accion de volumen:")
                        SelectorOpciones(
                            opciones = PasoMacro.AccionVolumen.entries.map { it.name to etiquetaVolumen(it) },
                            seleccionada = opcion,
                            onSeleccion = { opcion = it; error = null }
                        )
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val actualizado = construirPaso(paso, texto, texto2, opcion)
                if (actualizado == null) error = "Completa el campo antes de guardar."
                else onConfirmar(actualizado)
            }) { Text("Guardar paso") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun Nota(texto: String) {
    Text(texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SelectorOpciones(
    opciones: List<Pair<String, String>>,
    seleccionada: String,
    onSeleccion: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        opciones.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { (valor, etiqueta) ->
                    FilterChip(
                        selected = seleccionada == valor,
                        onClick = { onSeleccion(valor) },
                        label = { Text(etiqueta) }
                    )
                }
            }
        }
    }
}

private fun etiquetaDireccion(d: PasoMacro.Direccion): String = when (d) {
    PasoMacro.Direccion.ARRIBA -> "Arriba"
    PasoMacro.Direccion.ABAJO -> "Abajo"
    PasoMacro.Direccion.IZQUIERDA -> "Izquierda"
    PasoMacro.Direccion.DERECHA -> "Derecha"
}

private fun etiquetaMedios(a: PasoMacro.AccionMedios): String = when (a) {
    PasoMacro.AccionMedios.REPRODUCIR_PAUSAR -> "Play / Pausa"
    PasoMacro.AccionMedios.SIGUIENTE -> "Siguiente"
    PasoMacro.AccionMedios.ANTERIOR -> "Anterior"
}

private fun etiquetaVolumen(a: PasoMacro.AccionVolumen): String = when (a) {
    PasoMacro.AccionVolumen.SUBIR -> "Subir"
    PasoMacro.AccionVolumen.BAJAR -> "Bajar"
    PasoMacro.AccionVolumen.SILENCIAR -> "Silenciar"
}

private fun textoInicial(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Esperar -> paso.ms.toString()
    is PasoMacro.TocarPorTexto -> paso.texto
    is PasoMacro.EscribirTexto -> paso.contenido
    is PasoMacro.Navegar -> ""
    is PasoMacro.TocarCoordenada -> if (paso.x > 0f) paso.x.toInt().toString() else ""
    is PasoMacro.Deslizar -> ""
    is PasoMacro.AbrirApp -> paso.paquete
    is PasoMacro.ControlMedios -> ""
    is PasoMacro.AjustarVolumen -> ""
}

private fun textoSecundarioInicial(paso: PasoMacro): String = when (paso) {
    is PasoMacro.TocarCoordenada -> if (paso.y > 0f) paso.y.toInt().toString() else ""
    else -> ""
}

private fun opcionInicial(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Deslizar -> paso.direccion.name
    is PasoMacro.ControlMedios -> paso.accion.name
    is PasoMacro.AjustarVolumen -> paso.accion.name
    else -> ""
}

private fun tituloPaso(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Esperar -> "Editar espera"
    is PasoMacro.TocarPorTexto -> "Editar toque"
    is PasoMacro.EscribirTexto -> "Editar texto"
    is PasoMacro.Navegar -> "Paso de navegacion"
    is PasoMacro.TocarCoordenada -> "Editar toque por coordenada"
    is PasoMacro.Deslizar -> "Editar deslizamiento"
    is PasoMacro.AbrirApp -> "Editar app a abrir"
    is PasoMacro.ControlMedios -> "Editar control multimedia"
    is PasoMacro.AjustarVolumen -> "Editar volumen"
}

private fun construirPaso(
    original: PasoMacro,
    texto: String,
    texto2: String,
    opcion: String
): PasoMacro? = when (original) {
    is PasoMacro.Esperar -> texto.toLongOrNull()?.takeIf { it >= 0 }?.let { PasoMacro.Esperar(it) }
    is PasoMacro.TocarPorTexto -> texto.trim().takeIf { it.isNotEmpty() }?.let { PasoMacro.TocarPorTexto(it) }
    is PasoMacro.EscribirTexto -> texto.takeIf { it.isNotEmpty() }?.let { PasoMacro.EscribirTexto(it) }
    is PasoMacro.Navegar -> original
    is PasoMacro.TocarCoordenada -> {
        val x = texto.toFloatOrNull()
        val y = texto2.toFloatOrNull()
        if (x != null && y != null && x >= 0f && y >= 0f) PasoMacro.TocarCoordenada(x, y) else null
    }
    is PasoMacro.Deslizar ->
        runCatching { PasoMacro.Deslizar(PasoMacro.Direccion.valueOf(opcion)) }.getOrNull()
    is PasoMacro.AbrirApp ->
        texto.trim().takeIf { it.isNotEmpty() }?.let { PasoMacro.AbrirApp(it) }
    is PasoMacro.ControlMedios ->
        runCatching { PasoMacro.ControlMedios(PasoMacro.AccionMedios.valueOf(opcion)) }.getOrNull()
    is PasoMacro.AjustarVolumen ->
        runCatching { PasoMacro.AjustarVolumen(PasoMacro.AccionVolumen.valueOf(opcion)) }.getOrNull()
}
