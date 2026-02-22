package pt.pauloliveira.wradio.ui.explore

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.service.connection.PlayerState
import pt.pauloliveira.wradio.ui.common.StationLogo
import pt.pauloliveira.wradio.ui.navigation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by mainViewModel.playerState.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var selectedWrapper by remember { mutableStateOf<ExploreStationWrapper?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(modifier = Modifier.fillMaxSize()) {

        SearchSection(
            query = query,
            onQueryChange = { query = it },
            onSearch = {
                viewModel.search(query)
                focusManager.clearFocus()
            }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ExploreUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.explore_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is ExploreUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is ExploreUiState.Success -> {
                    RemoteStationList(
                        wrappers = state.stations,
                        onItemClick = { wrapper ->
                            selectedWrapper = wrapper
                        }
                    )
                }

                is ExploreUiState.Error.NoResults -> {
                    ErrorDisplay(
                        message = stringResource(R.string.error_no_results, state.query)
                    )
                }

                is ExploreUiState.Error.Network -> {
                    ErrorDisplay(
                        message = stringResource(R.string.error_network, state.message)
                    )
                }
            }
        }
    }

    if (selectedWrapper != null) {
        StationDetailSheet(
            wrapper = selectedWrapper!!,
            playerState = playerState,
            sheetState = sheetState,
            onDismissRequest = { selectedWrapper = null },
            onPreviewClick = {
                val isPlayingThis = playerState.station?.uuid == it.uuid && playerState.isPlaying
                if (isPlayingThis) {
                    mainViewModel.togglePlayPause()
                } else {
                    viewModel.previewStation(it)
                }
            },
            onImportClick = {
                viewModel.importStation(it)
                selectedWrapper = null
            }
        )
    }
}

@Composable
fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.explore_search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            }
        )
    }
}

@Composable
fun ErrorDisplay(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RemoteStationList(
    wrappers: List<ExploreStationWrapper>,
    onItemClick: (ExploreStationWrapper) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(wrappers) { wrapper ->
            RemoteStationItem(wrapper, onItemClick)
        }
    }
}

@Composable
fun RemoteStationItem(
    wrapper: ExploreStationWrapper,
    onClick: (ExploreStationWrapper) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(wrapper) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationLogo(
            url = wrapper.station.stationLogo,
            uuid = wrapper.station.uuid,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = wrapper.station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subText = listOfNotNull(
                wrapper.station.countryCode,
                wrapper.station.tags.firstOrNull()
            ).joinToString(" • ")

            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (wrapper.status == StationStatus.Saved) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailSheet(
    wrapper: ExploreStationWrapper,
    playerState: PlayerState,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onPreviewClick: (Station) -> Unit,
    onImportClick: (Station) -> Unit
) {
    val station = wrapper.station
    val context = LocalContext.current
    val isCurrentStation = playerState.station?.uuid == station.uuid
    val isPlaying = isCurrentStation && playerState.isPlaying
    val isBuffering = isCurrentStation && playerState.isBuffering

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                StationLogo(
                    url = station.stationLogo,
                    uuid = station.uuid,
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitle = listOfNotNull(
                        station.countryCode,
                        station.tags.take(3).joinToString(", ")
                    ).joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = stringResource(R.string.lbl_bitrate),
                    value = if (station.bitrate > 0) "${station.bitrate} kbps" else "-"
                )
                InfoColumn(
                    label = stringResource(R.string.lbl_codec),
                    value = station.codec ?: "-"
                )
                InfoColumn(
                    label = stringResource(R.string.lbl_votes),
                    value = station.votes.toString()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!station.homepage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, station.homepage.toUri())
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore invalid URLs
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lbl_homepage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalButton(
                    onClick = { onPreviewClick(station) },
                    modifier = Modifier.weight(1f),
                    enabled = !isBuffering
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.state_buffering))
                    } else if (isPlaying) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_stop))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_preview))
                    }
                }

                Button(
                    onClick = { onImportClick(station) },
                    modifier = Modifier.weight(1f),
                    enabled = wrapper.status != StationStatus.Saved
                ) {
                    val icon =
                        if (wrapper.status == StationStatus.Saved) Icons.Default.Check else Icons.Default.Add
                    val text = if (wrapper.status == StationStatus.Saved)
                        stringResource(R.string.status_saved)
                    else
                        stringResource(R.string.action_import)

                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text)
                }
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}