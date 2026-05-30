package pt.pauloliveira.wradio.data.content

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import pt.pauloliveira.wradio.data.local.WRadioDatabase
import java.io.File

class LogoContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "pt.pauloliveira.wradio.logos"

        fun getLogoUri(uuid: String): Uri =
            Uri.parse("content://$AUTHORITY/$uuid")
    }

    private val database by lazy {
        Room.databaseBuilder(
            context!!.applicationContext,
            WRadioDatabase::class.java,
            "wradio_db"
        ).fallbackToDestructiveMigration().build()
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val uuid = uri.lastPathSegment ?: return null
        val blob = runBlocking {
            database.stationDao.getLogoBlob(uuid)
        } ?: return null

        val cacheDir = File(context!!.cacheDir, "logo_cache").apply { mkdirs() }
        val tempFile = File(cacheDir, "$uuid.webp")
        tempFile.writeBytes(blob)
        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/webp"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
