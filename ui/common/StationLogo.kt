package pt.pauloliveira.wradio.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.absoluteValue

@Composable
fun StationLogo(
    url: String?,
    uuid: String,
    modifier: Modifier = Modifier,
    shape: Shape
) {
    val dynamicColor = remember(uuid) {
        val hue = (uuid.hashCode() % 360).absoluteValue.toFloat()
        Color.hsv(hue, 0.65f, 0.8f)
    }

    Box(modifier = modifier.clip(shape)) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ColoredPlaceholder(dynamicColor) },
            error = { ColoredPlaceholder(dynamicColor) }
        )
    }
}

@Composable
private fun ColoredPlaceholder(backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Radio,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(8.dp).fillMaxSize(0.6f)
        )
    }
}