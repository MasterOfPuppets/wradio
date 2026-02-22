package pt.pauloliveira.wradio.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.ui.common.StationLogo

@Composable
fun ManagementScreen(
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val stations by viewModel.stations.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val msgSaved = stringResource(R.string.msg_station_saved)
    val msgDeleted = stringResource(R.string.msg_station_deleted)
    var showDialog by remember { mutableStateOf(false) }
    var stationToEdit by remember { mutableStateOf<Station?>(null) }
   var stationToDelete by remember { mutableStateOf<Station?>(null) }
   if (showDialog) {
        AddEditStationDialog(
            stationToEdit = stationToEdit,
            onDismiss = { showDialog = false },
            onSave = { name, url ->
                if (stationToEdit == null) {
                    viewModel.addManualStation(name, url)
                } else {
                    viewModel.updateStation(stationToEdit!!, name, url)
                }
                showDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(msgSaved)
                }
            }
        )
    }
    if (stationToDelete != null) {
        AlertDialog(
            onDismissRequest = { stationToDelete = null },
            title = {
                Text(text = stringResource(R.string.dialog_delete_title))
            },
            text = {
                Column {
                    Text(text = stringResource(R.string.dialog_delete_message))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stationToDelete!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStation(stationToDelete!!)
                        stationToDelete = null

                        scope.launch {
                            snackbarHostState.showSnackbar(msgDeleted)
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.action_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { stationToDelete = null }
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                stationToEdit = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dialog_title_add))
            }
        }
    ) { paddingValues ->
        ManagementList(
            stations = stations,
            padding = paddingValues,
            onEditClick = { station ->
                stationToEdit = station
                showDialog = true
            },
            onDeleteClick = { station ->
                stationToDelete = station
            }
        )
    }
}

@Composable
fun ManagementList(
    stations: List<Station>,
    padding: PaddingValues,
    onEditClick: (Station) -> Unit,
    onDeleteClick: (Station) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stations) { station ->
            ManagementItem(station, onEditClick, onDeleteClick)
        }
    }
}

@Composable
fun ManagementItem(
    station: Station,
    onEditClick: (Station) -> Unit,
    onDeleteClick: (Station) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(
                url = station.stationLogo,
                uuid = station.uuid,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.streamUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (station.isManuallyAdded) {
                    Text(
                        text = stringResource(R.string.tag_manual),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                IconButton(onClick = { onEditClick(station) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.dialog_title_edit))
                }
                IconButton(onClick = { onDeleteClick(station) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}