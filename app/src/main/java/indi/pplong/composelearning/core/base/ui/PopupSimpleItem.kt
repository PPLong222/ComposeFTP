package indi.pplong.composelearning.core.base.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Description:
 * @author PPLong
 * @date 10/29/24 8:54 PM
 */
@Composable
@Preview
fun PopupSimpleItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Default.Home,
    text: String = "123",
    onclick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                onclick()
            }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = imageVector, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text, color = MaterialTheme.colorScheme.onSurface)
            HorizontalDivider()
        }
    }
}