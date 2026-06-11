package com.omnisciente.macro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
                        Text("Pausa antes del siguiente paso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    is PasoMacro.TocarPorTexto -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it; error = null },
                            label = { Text("Texto del boton o elemento") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("La macro buscara un elemento con este texto y lo tocara.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    is PasoMacro.EscribirTexto -> {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it; error = null },
                            label = { Text("Texto a escribir") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Se escribe en el campo que este enfocado en ese momento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    is PasoMacro.Navegar -> {
                        Text("Paso de navegacion; no requiere edicion de texto.")
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
                val actualizado = construirPaso(paso, texto)
                if (actualizado == null) error = "Completa el campo antes de guardar."
                else onConfirmar(actualizado)
            }) { Text("Guardar paso") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

private fun textoInicial(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Esperar -> paso.ms.toString()
    is PasoMacro.TocarPorTexto -> paso.texto
    is PasoMacro.EscribirTexto -> paso.contenido
    is PasoMacro.Navegar -> ""
}

private fun tituloPaso(paso: PasoMacro): String = when (paso) {
    is PasoMacro.Esperar -> "Editar espera"
    is PasoMacro.TocarPorTexto -> "Editar toque"
    is PasoMacro.EscribirTexto -> "Editar texto"
    is PasoMacro.Navegar -> "Paso de navegacion"
}

private fun construirPaso(original: PasoMacro, texto: String): PasoMacro? = when (original) {
    is PasoMacro.Esperar -> texto.toLongOrNull()?.takeIf { it >= 0 }?.let { PasoMacro.Esperar(it) }
    is PasoMacro.TocarPorTexto -> texto.trim().takeIf { it.isNotEmpty() }?.let { PasoMacro.TocarPorTexto(it) }
    is PasoMacro.EscribirTexto -> texto.takeIf { it.isNotEmpty() }?.let { PasoMacro.EscribirTexto(it) }
    is PasoMacro.Navegar -> original
}
