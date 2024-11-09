package indi.pplong.composelearning.core.base.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import indi.pplong.composelearning.core.util.dpToPx
import indi.pplong.composelearning.core.util.isImageFile
import indi.pplong.composelearning.core.util.isMediaWithThumbnail
import kotlinx.coroutines.launch

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 5:46 PM
 */

@Composable
fun LocalFileAsyncImage(
    uri: Uri, size: Int = 48, cornerSize: Int = 8,
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (uri.isMediaWithThumbnail(context)) {
        LaunchedEffect(uri) {
            scope.launch {
//                if (uri.isImageFile(context)) {
//                    bitmap = null.  2
//                    isLoading = false
//                } else {
//                    withContext(Dispatchers.IO) {text)
//                        withContext(Dispatchers.Main) {
//                            bitmap = thumbBitmap
//                            isLoading = true
//                        }
//                    }
//                }
            }
        }

        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(cornerSize.dp))
                .shimmerEffect()
        ) {
            AsyncImage(
                imageLoader = context.imageLoader,
                model = ImageRequest.Builder(context)
                    .data(if (uri.isImageFile(context)) uri.toString() else bitmap).crossfade(true)
                    .size(size.dpToPx(context))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    } else {
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(cornerSize.dp))
        )
    }

}