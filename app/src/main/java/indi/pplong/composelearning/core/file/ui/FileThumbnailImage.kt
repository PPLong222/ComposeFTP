package indi.pplong.composelearning.core.file.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import indi.pplong.composelearning.core.base.ui.shimmerEffect
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.util.dpToPx

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
    // TODO: Add to param (48 dp is the default)
    val size = 48.dpToPx(context)
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(localUri)
            .memoryCacheKey(key)
            .size(size)
            .build()
    )
    LaunchedEffect(localUri, key) {
        if (!GlobalCacheList.map.containsKey(key)) {
            Log.d("ttt", "FileThumbnailAsyncImage: not cached $key")
            cache()
        } else {
            Log.d("tttt", "FileThumbnailAsyncImage: $key - ${localUri}")
        }
    }
    if (localUri == "") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    } else {
        when (painter.state) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading -> {
                Log.d("ttt", "FileThumbnailAsyncImage: Loading")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }

            is AsyncImagePainter.State.Success -> {
                Log.d("ttt", "FileThumbnailAsyncImage: Success")
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

            }

            is AsyncImagePainter.State.Error -> {
                Log.d("ttt", "FileThumbnailAsyncImage: Error")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            }
        }
    }

}