package pt.pauloliveira.wradio.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.BuildConfig
import pt.pauloliveira.wradio.R

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appVersion = BuildConfig.VERSION_NAME
    val bufferSize by viewModel.bufferSize.collectAsState()
    val duckLevel by viewModel.duckLevel.collectAsState()
    val bluetoothAutoPause by viewModel.bluetoothAutoPause.collectAsState()
    val preferredAudioDeviceName by viewModel.preferredAudioDeviceName.collectAsState()
    val knownBluetoothDevices by viewModel.knownBluetoothDevices.collectAsState()
    val sourcesRefreshState by viewModel.sourcesRefreshState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val exportImportState by viewModel.exportImportState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportListName by rememberSaveable { mutableStateOf(viewModel.generateExportName()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val name = exportListName.trim().ifBlank { viewModel.generateExportName() }
            viewModel.exportStations(uri, name)
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importStations(uri)
        }
    }

    LaunchedEffect(exportImportState) {
        when (val state = exportImportState) {
            is ExportImportState.ExportSuccess -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_export_success, state.count),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearExportImportState()
            }

            is ExportImportState.ImportSuccess -> {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.msg_import_success,
                        state.result.added,
                        state.result.skipped
                    ),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearExportImportState()
            }

            is ExportImportState.Error -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_export_error, state.message),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearExportImportState()
            }

            ExportImportState.Idle -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(title = stringResource(R.string.settings_group_playback)) {
            SettingsItem(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.settings_buffer_size),
                subtitle = stringResource(R.string.label_seconds, bufferSize),
                onClick = { showBufferDialog = true }
            )
            Text(
                text = stringResource(R.string.settings_buffer_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            SettingsToggleItem(
                icon = Icons.Default.Bluetooth,
                title = stringResource(R.string.settings_bt_pause),
                subtitle = stringResource(R.string.settings_bt_pause_desc),
                checked = bluetoothAutoPause,
                onCheckedChange = { viewModel.saveBluetoothAutoPause(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup(title = stringResource(R.string.settings_group_about)) {
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = when (updateState) {
                    is UpdateState.Checking -> "$appVersion (${stringResource(R.string.settings_update_checking)})"
                    is UpdateState.UpToDate -> "$appVersion (${stringResource(R.string.settings_update_up_to_date)})"
                    is UpdateState.Available -> "$appVersion  ${(updateState as UpdateState.Available).update.latestVersion}"
                    else -> appVersion
                },
                onClick = {
                    val state = updateState
                    if (state is UpdateState.Available) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, state.update.downloadUrl.toUri())
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    } else {
                        viewModel.checkForUpdate()
                    }
                }
            )
            if (updateState is UpdateState.Available) {
                SettingsItem(
                    icon = Icons.Default.NewReleases,
                    title = stringResource(R.string.settings_update_available),
                    subtitle = stringResource(R.string.settings_update_tap_to_download),
                    onClick = {
                        val state = updateState
                        if (state is UpdateState.Available) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, state.update.downloadUrl.toUri())
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    }
                )
            }
            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_terms),
                subtitle = null,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/".toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup(title = stringResource(R.string.settings_group_sources)) {
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_update_sources),
                subtitle = when (sourcesRefreshState) {
                    is SourcesRefreshState.Loading -> stringResource(R.string.settings_sources_checking)
                    is SourcesRefreshState.Updated -> stringResource(R.string.settings_sources_updated)
                    is SourcesRefreshState.AlreadyUpToDate -> stringResource(R.string.settings_sources_up_to_date)
                    else -> stringResource(R.string.settings_sources_desc)
                },
                onClick = { viewModel.refreshSources() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup(title = stringResource(R.string.settings_group_data)) {
            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_export_list),
                subtitle = stringResource(R.string.settings_export_list_desc),
                onClick = {
                    exportListName = viewModel.generateExportName()
                    showExportDialog = true
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_import_list),
                subtitle = stringResource(R.string.settings_import_list_desc),
                onClick = { importDocumentLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.settings_clear_data),
                subtitle = stringResource(R.string.settings_clear_data_desc),
                isDestructive = true,
                onClick = { showDeleteDialog = true }
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.dialog_export_title)) },
            text = {
                OutlinedTextField(
                    value = exportListName,
                    onValueChange = { exportListName = it },
                    label = { Text(stringResource(R.string.dialog_export_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        createDocumentLauncher.launch(buildExportFileName(exportListName))
                    }
                ) {
                    Text(stringResource(R.string.action_export))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showBufferDialog) {
        BufferSelectionDialog(
            currentValue = bufferSize,
            onDismiss = { showBufferDialog = false },
            onSelected = {
                viewModel.saveBufferSize(it)
                showBufferDialog = false
            }
        )
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun buildExportFileName(name: String): String {
    val sanitized = name.trim()
        .ifBlank { "wradio-stations" }
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    return if (sanitized.lowercase().endsWith(".json")) sanitized else "$sanitized.json"
}

@Composable
fun PreferredDeviceDialog(
    currentValue: String,
    knownDevices: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentValue) }
    val options = listOf("") + knownDevices.sorted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_preferred_device_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.dialog_preferred_device_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { name ->
                    val label = if (name.isBlank())
                        stringResource(R.string.settings_preferred_audio_device_value_default)
                    else name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (name == selected),
                                onClick = { selected = name },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (name == selected), onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelected(selected) }) {
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

@Composable
fun DuckLevelDialog(
    currentValue: Float,
    onDismiss: () -> Unit,
    onSelected: (Float) -> Unit
) {
    var sliderValue by rememberSaveable { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_duck_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_duck_level_value, (sliderValue * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..1f,
                    steps = 19
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelected(sliderValue) }) {
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

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun BufferSelectionDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onSelected: suspend (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val options = listOf(10, 30, 60, 120)

    var selectedValue by remember { mutableIntStateOf(currentValue) }
    var showRestartConfirmation by remember { mutableStateOf(false) }

    if (showRestartConfirmation) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_restart_title)) },
            text = { Text(stringResource(R.string.dialog_restart_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onSelected(selectedValue)
                            triggerAppRestart(context)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_restart_app))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_later))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_buffer_title)) },
            text = {
                Column {
                    options.forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (seconds == selectedValue),
                                    onClick = {
                                        selectedValue = seconds
                                        if (selectedValue != currentValue) {
                                            showRestartConfirmation = true
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (seconds == selectedValue),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.label_seconds, seconds),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun triggerAppRestart(context: android.content.Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}
