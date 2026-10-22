package com.vibe.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vibe.app.BuildConfig
import com.vibe.app.data.api.AuthStore
import okhttp3.OkHttpClient

/** Image loader that shows the server's private photos: every request carries the login token, the server decides if it may be opened. */
object AuthImages {
    private var loader: ImageLoader? = null

    fun get(ctx: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(ctx.applicationContext)
            .okHttpClient {
                OkHttpClient.Builder().addInterceptor { chain ->
                    val t = AuthStore.accessToken
                    chain.proceed(if (t != null && chain.request().url.toString().startsWith(BuildConfig.API_BASE)) chain.request().newBuilder().header("Authorization", "Bearer $t").build() else chain.request())
                }.build()
            }
            .components { add(coil.decode.VideoFrameDecoder.Factory()) }   // first frame of a video as its picture
            .crossfade(true)
            .build().also { loader = it }
    }

    /** After sign-out nothing of the last person's photos may stay. */
    fun clear() {
        loader?.memoryCache?.clear()
        loader?.diskCache?.clear()
    }

    /** Full address of a server path ("/media/5/file") or an address that is already complete. */
    fun url(pathOrUrl: String) = if (pathOrUrl.startsWith("http")) pathOrUrl else BuildConfig.API_BASE + pathOrUrl
}

/** A server photo (public DP or private media path) cropped to fill its parent. [focusY] -1 top .. 1 bottom. */
@Composable
fun ServerImage(pathOrUrl: String, modifier: Modifier = Modifier, focusY: Float = -0.3f, scale: ContentScale = ContentScale.Crop) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx).data(AuthImages.url(pathOrUrl)).memoryCachePolicy(CachePolicy.ENABLED).build(),
        contentDescription = null, imageLoader = AuthImages.get(ctx), modifier = modifier,
        contentScale = scale, alignment = androidx.compose.ui.BiasAlignment(0f, focusY),
    )
}
