package com.vibe.app.data.api

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.vibe.app.ui.components.PickedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Gets a picked or captured file ready to send: photos are turned upright and shrunk (phones take 8 MB photos, the server wants a few). */
object Uploads {
    /** A photo as JPEG bytes, longest side at most 2000 px. */
    suspend fun photoBytes(ctx: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val local = if (uri.scheme == "file") File(uri.path.orEmpty()) else PickedMedia.copy(ctx, uri, "up_", keepOnlyLatest = true)?.path?.let { File(it) }
        val bmp = local?.let { PickedMedia.load(it.path, false, 2000) } ?: throw ApiException(0, "read_failed", "Could not read that photo")
        ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
    }

    /** A video as it is on the phone (the server checks size and length). */
    suspend fun videoBytes(ctx: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ApiException(0, "read_failed", "Could not read that video")
    }
}
