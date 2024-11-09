package indi.pplong.composelearning.core.file.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.cache.GlobalCacheList

/**
 * Description:
 * @author PPLong
 * @date 11/4/24 4:45 PM
 */

@Composable
fun FileThumbnailAsyncImage(
    key: String,
    localUri: String,
    cache: () -> Unit
) {
    val context = LocalContext.current
    var isImageLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!GlobalCacheList.map.containsKey(key)) {
            Log.d("ttt", "FileThumbnailAsyncImage: not cached $key")
            Log.d("ttt", "FileThumbnailAsyncImage: ${GlobalCacheList.map.keys().toList()}")
            cache()
        }
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(localUri)
            .diskCacheKey(key)
            .memoryCacheKey(key)
            .crossfade(true)
            .error(R.drawable.ic_videocam)
            .transformations(RoundedCornersTransformation(8.0F))
            .build(),
        contentDescription = null,
        imageLoader = context.imageLoader,
        onSuccess = { isImageLoaded = true },
        onError = {},
        onLoading = {}
    )
}