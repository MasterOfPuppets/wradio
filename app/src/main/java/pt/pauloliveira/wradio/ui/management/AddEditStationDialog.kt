package pt.pauloliveira.wradio.ui.management

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.model.Station

@Composable
fun AddEditStationDialog(
    stationToEdit: Station? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    val isUrlValid by remember(url) {
        derivedStateOf {
            url.isNotBlank()
        }
    }

    val canSave by remember(name, url, isUrlValid) {
        derivedStateOf {
            name.isNotBlank() && url.isNotBlank() && isUrlValid
        }
    }

    LaunchedEffect(stationToEdit) {
        if (stationToEdit != null) {
            name = stationToEdit.name
            url = stationToEdit.streamUrl
        } else {
            name = ""
            url = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val titleRes =
                if (stationToEdit == null) R.string.dialog_title_add else R.string.dialog_title_edit
            Text(text = stringResource(titleRes))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text(stringResource(R.string.dialog_label_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = !isUrlValid && url.isNotEmpty(),
                    supportingText = {
                        if (!isUrlValid && url.isNotEmpty()) {
                            Text(stringResource(R.string.error_invalid_url))
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(name, url)
                    }
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}