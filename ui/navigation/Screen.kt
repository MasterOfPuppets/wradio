package pt.pauloliveira.wradio.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import pt.pauloliveira.wradio.R

sealed class Screen(
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector
) {
    object Explore : Screen("explore", R.string.nav_explore, Icons.Default.Search)

    object MyRadio : Screen("my_radio", R.string.nav_my_radio, Icons.Default.Radio)

    object Management : Screen(
        "management", R.string.nav_management,
        Icons.AutoMirrored.Filled.List
    )

    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}