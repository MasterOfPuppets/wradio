package pt.pauloliveira.wradio.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.ui.common.StationLogo

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val stations by viewModel.stations.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(
            sortField = sortField,
            sortDirection = sortDirection,
            onSortFieldClick = { viewModel.toggleSortField() },
            onSortDirectionClick = { viewModel.toggleSortDirection() }
        )

        if (stations.isEmpty()) {
            HomeEmptyState(
                modifier = Modifier.weight(1f),
                onAddSamples = { viewModel.addSampleStations() }
            )
        } else {
            StationList(
                modifier = Modifier.weight(1f),
                stations = stations,
                onStationClick = { station ->
                    viewModel.play(station)
                }
            )
        }
    }
}

@Composable
fun HomeHeader(
    sortField: HomeViewModel.SortField,
    sortDirection: HomeViewModel.SortDirection,
    onSortFieldClick: () -> Unit,
    onSortDirectionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onSortFieldClick) {
            val fieldIcon = if (sortField == HomeViewModel.SortField.Name) {
                Icons.Default.SortByAlpha
            } else {
                Icons.Default.Schedule
            }
            val fieldDescription = if (sortField == HomeViewModel.SortField.Name) {
                stringResource(R.string.cd_sort_field_name)
            } else {
                stringResource(R.string.cd_sort_field_total_play_time)
            }
            Icon(imageVector = fieldIcon, contentDescription = fieldDescription)
        }

        IconButton(onClick = onSortDirectionClick) {
            val directionIcon = if (sortDirection == HomeViewModel.SortDirection.Asc) {
                Icons.Default.ArrowUpward
            } else {
                Icons.Default.ArrowDownward
            }
            val directionDescription = if (sortDirection == HomeViewModel.SortDirection.Asc) {
                stringResource(R.string.cd_sort_direction_asc)
            } else {
                stringResource(R.string.cd_sort_direction_desc)
            }
            Icon(imageVector = directionIcon, contentDescription = directionDescription)
        }
    }
}

@Composable
fun HomeEmptyState(
    modifier: Modifier = Modifier,
    onAddSamples: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.home_empty_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = onAddSamples) {
                Text(text = stringResource(R.string.action_add_sample_stations))
            }
        }
    }
}

@Composable
fun StationList(
    modifier: Modifier = Modifier,
    stations: List<Station>,
    onStationClick: (Station) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stations) { station ->
            StationItem(station, onStationClick)
        }
    }
}

@Composable
fun StationItem(
    station: Station,
    onClick: (Station) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(station) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(
                logoBlob = station.logoBlob,
                uuid = station.uuid,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (station.tags.isNotEmpty()) {
                    Text(
                        text = station.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}