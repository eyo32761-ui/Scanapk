package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CourierDetector

@Composable
fun ManualInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (code: String, format: String) -> Unit
) {
    var codeText by remember { mutableStateOf("") }
    val detected = remember(codeText) {
        if (codeText.isNotBlank()) CourierDetector.detect(codeText) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("manual_input_dialog"),
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Input Kode Manual", fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ketik atau tempel nomor resi paket / barcode:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_code_textfield"),
                    placeholder = { Text("Contoh: JP1234567890", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (codeText.isNotBlank()) {
                                onSubmit(codeText.trim(), "CODE_128")
                            }
                        }
                    )
                )

                if (detected != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CourierBadge(
                        courierName = detected.courierName,
                        category = detected.category
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeText.isNotBlank()) {
                        onSubmit(codeText.trim(), "CODE_128")
                    }
                },
                enabled = codeText.isNotBlank(),
                modifier = Modifier.testTag("manual_submit_button")
            ) {
                Text("Proses Scan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("manual_cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}
