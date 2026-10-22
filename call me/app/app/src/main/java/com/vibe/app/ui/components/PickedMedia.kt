package com.vibe.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The signed-in user's own DP (profile picture), picked from the phone gallery. Shown on Me, My status and Edit profile. */
object MyProfile {
    var dp by mutableStateOf<Uri?>(null)

    /** Call once at start: a DP picked earlier is kept in the app folder. */
    fun restore(ctx: Context) {
        if (dp != null) return
        ctx.filesDir.listFiles { f -> f.name.startsWith("dp_") }?.maxByOrNull { it.lastModified() }?.let { dp = Uri.fromFile(it) }
    }
}

object PickedMedia {
    /** Copies a picked gallery file into the app folder (the picker only gives short-lived access) and returns its Uri. */
    fun copy(ctx: Context, uri: Uri, prefix: String, keepOnlyLatest: Boolean = false): Uri? = try {
        if (keepOnlyLatest) ctx.filesDir.listFiles { f -> f.name.startsWith(prefix) }?.forEach { it.delete() }
        val f = File(ctx.filesDir, prefix + System.currentTimeMillis())
        ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        Uri.fromFile(f)
    } catch (e: Exception) { null }

    fun isVideo(ctx: Context, uri: Uri): Boolean = ctx.contentResolver.getType(uri)?.startsWith("video") == true

    fun load(path: String, video: Boolean, maxSide: Int = 1280): Bitmap? = try {
        if (video) {
            val r = MediaMetadataRetriever()
            r.setDataSource(path)
            r.getFrameAtTime(0).also { r.release() }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
            val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            val rot = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (bmp != null && rot != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true) else bmp
        }
    } catch (e: Exception) { null }
}

/** Shows a picked photo (or the first frame of a picked video), cropped to fill. */
@Composable
fun UriImage(uri: Uri?, modifier: Modifier = Modifier, video: Boolean = false) {
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = uri?.path?.let { p -> withContext(Dispatchers.IO) { PickedMedia.load(p, video)?.asImageBitmap() } }
    }
    bmp?.let { Image(it, null, modifier, contentScale = ContentScale.Crop) }
}

/** The signed-in user's DP: the copy saved on this phone, else the server's photo, else the initial. */
@Composable
fun MyDp(modifier: Modifier, initial: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val local = MyProfile.dp
    val remote = com.vibe.app.data.api.AuthStore.me?.optString("dp")?.takeIf { it.startsWith("http") }
    androidx.compose.foundation.layout.Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        when {
            local != null -> UriImage(local, androidx.compose.ui.Modifier.matchParentSize())
            remote != null -> coil.compose.AsyncImage(remote, null, androidx.compose.ui.Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            else -> androidx.compose.material3.Text(initial, color = com.vibe.app.ui.theme.VibeColors.Pink, fontSize = fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Light)
        }
    }
}
