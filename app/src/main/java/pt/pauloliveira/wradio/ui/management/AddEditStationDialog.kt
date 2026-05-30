package pt.pauloliveira.wradio.ui.management

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.ui.common.StationLogo
import java.io.ByteArrayOutputStream
import java.net.URL

private const val LOGO_SIZE = 128

private fun resizeAndCompressImage(context: Context, sourceUri: Uri): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val bufferedStream = inputStream.buffered()
        bufferedStream.mark(Int.MAX_VALUE)
        BitmapFactory.decodeStream(bufferedStream, null, options)
        bufferedStream.reset()

        val minDim = minOf(options.outWidth, options.outHeight)
        var inSampleSize = 1
        while (minDim / inSampleSize > LOGO_SIZE * 2) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val sampled = BitmapFactory.decodeStream(bufferedStream, null, decodeOptions)
        bufferedStream.close()

        if (sampled == null) return null

        val cropSize = minOf(sampled.width, sampled.height)
        val xOffset = (sampled.width - cropSize) / 2
        val yOffset = (sampled.height - cropSize) / 2
        val cropped = Bitmap.createBitmap(sampled, xOffset, yOffset, cropSize, cropSize)
        val resized = Bitmap.createScaledBitmap(cropped, LOGO_SIZE, LOGO_SIZE, true)

        if (cropped != sampled) sampled.recycle()
        if (resized != cropped) cropped.recycle()

        val outputStream = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        resized.compress(format, 80, outputStream)
        resized.recycle()

        outputStream.toByteArray()
    } catch (_: Exception) {
        null
    }
}

private suspend fun downloadAndCompressFromUrl(url: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection().apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        val bytes = connection.getInputStream().use { it.readBytes() }
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

        val cropSize = minOf(original.width, original.height)
        val xOffset = (original.width - cropSize) / 2
        val yOffset = (original.height - cropSize) / 2
        val cropped = Bitmap.createBitmap(original, xOffset, yOffset, cropSize, cropSize)
        val resized = Bitmap.createScaledBitmap(cropped, LOGO_SIZE, LOGO_SIZE, true)

        if (cropped != original) original.recycle()
        if (resized != cropped) cropped.recycle()

        val outputStream = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        resized.compress(format, 80, outputStream)
        resized.recycle()

        outputStream.toByteArray()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AddEditStationDialog(
    stationToEdit: Station? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, logoBlob: ByteArray?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var logoBlob by remember { mutableStateOf<ByteArray?>(null) }
    var logoUrlInput by remember { mutableStateOf("") }
    var isLoadingLogo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val blob = resizeAndCompressImage(context, uri)
            if (blob != null) {
                logoBlob = blob
            }
        }
    }

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
            logoBlob = stationToEdit.logoBlob
        } else {
            name = ""
            url = ""
            logoBlob = null
        }
        logoUrlInput = ""
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationLogo(
                        logoBlob = logoBlob,
                        uuid = stationToEdit?.uuid ?: "new",
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.action_choose_gallery))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = logoUrlInput,
                        onValueChange = { logoUrlInput = it },
                        label = { Text(stringResource(R.string.dialog_label_logo_url)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isLoadingLogo) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(
                            onClick = {
                                if (logoUrlInput.isNotBlank()) {
                                    isLoadingLogo = true
                                    scope.launch {
                                        val blob = downloadAndCompressFromUrl(logoUrlInput)
                                        if (blob != null) {
                                            logoBlob = blob
                                        }
                                        isLoadingLogo = false
                                    }
                                }
                            },
                            enabled = logoUrlInput.isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_load_logo))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        onSave(name, url, logoBlob)
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