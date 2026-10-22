package com.vibe.app.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Turns a picked photo into a small upright JPEG before it leaves the phone. Re-encoding also drops the EXIF data
 * (camera, GPS location), so no hidden location is uploaded with a profile photo.
 */
object DpUpload {
    suspend fun jpegBytes(ctx: Context, uri: Uri, maxSide: Int = 1080): ByteArray = withContext(Dispatchers.IO) {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: throw ApiException(0, "not_an_image", "Could not read that photo")
        val rot = runCatching {
            cr.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        if (rot != 0f || scale < 1f) {
            val m = Matrix().apply { postRotate(rot); postScale(scale, scale) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
    }

    /** Uploads the photo as the user's DP and stores the updated user. */
    suspend fun upload(ctx: Context, uri: Uri) {
        val res = Api.upload("/me/dp", "photo", "dp.jpg", "image/jpeg", jpegBytes(ctx, uri))
        AuthStore.saveUser(res.getJSONObject("user"))
    }
}
