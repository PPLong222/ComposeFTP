package indi.pplong.composelearning.core.base.ui

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.util.dpToPx
import indi.pplong.composelearning.core.util.isMediaWithThumbnail

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 5:46 PM
 */

@Composable
@Preview
fun LocalFileAsyncImage(
    uri: Uri = "".toUri(), size: Int = 40, cornerSize: Int = 8,
) {
    val context = LocalContext.current
    if (uri.isMediaWithThumbnail(context)) {
        AsyncImage(
            imageLoader = context.imageLoader,
            model = ImageRequest.Builder(context)
                .data(uri.toString())
                .crossfade(true)
                .size(size.dpToPx(context))
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

    } else {
        Icon(
            painter = painterResource(R.drawable.ic_description),
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(cornerSize.dp))
        )
    }
}