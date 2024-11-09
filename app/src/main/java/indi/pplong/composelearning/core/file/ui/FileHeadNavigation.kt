package indi.pplong.composelearning.core.file.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:04 PM
 */

@Composable
fun HeadPathNavigation(
    path: String, modifier: Modifier = Modifier, onPathClick: (String) -> Unit = {},
) {

    val pathURI = Uri.parse(path)
    val rowState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(pathURI) {
        coroutineScope.launch {
            if (pathURI.pathSegments.isNotEmpty()) {
                rowState.animateScrollToItem(pathURI.pathSegments.lastIndex)
            }
        }
    }
    LazyRow(
        state = rowState,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.clickable {
                    onPathClick("/")
                },
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        items(pathURI.pathSegments) { segment ->
            SinglePathText(segment, isCurPath = pathURI.pathSegments.last() == segment) {
                onPathClick(
                    buildPath(
                        pathURI.pathSegments.indexOf(segment),
                        pathURI
                    )
                )
            }
        }
    }
}

fun buildPath(index: Int, previousUri: Uri): String {
    val uri = Uri.Builder()
    for (i in 0..index) {
        uri.appendPath(previousUri.pathSegments[i])
    }
    return uri.build().path ?: "/"
}

@Composable
fun SinglePathText(
    pathText: String, isCurPath: Boolean = false, onButtonClick: () -> Unit = {}
) {
    val source = remember { MutableInteractionSource() }
    val ripple = ripple(color = Color.Gray, bounded = true) // 设置涟漪效果的颜色

    Card(
        shape = RoundedCornerShape(size = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(size = 8.dp))
            .clickable(
                interactionSource = source,
                indication = ripple,
                onClick = onButtonClick
            )
    ) {
        Text(
            text = "/".plus(pathText),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurPath) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Preview
@Composable
fun PreviewHeadPathNavigation() {
    val pathURI = Uri.parse("/abc/asd/aass/a")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeadPathNavigation("/abc/asd/aassssssssss/aaasdasdasdasda/12312/12312312") {}

        UploadButton()

    }
}