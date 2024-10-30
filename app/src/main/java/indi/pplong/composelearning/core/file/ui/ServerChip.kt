package indi.pplong.composelearning.core.file.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 4:09 PM
 */

@Composable
fun ServerChip(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    label: String = "",
    delete: () -> Unit = {}
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null
            )
        },
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.clickable {
                    onClick.invoke()
                }
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    )
}

@Composable
@Preview
fun PreviewServerChip() {
    ServerChip(label = "123123")
}