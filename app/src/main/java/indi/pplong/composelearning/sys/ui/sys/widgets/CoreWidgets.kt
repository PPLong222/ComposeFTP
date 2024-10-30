package indi.pplong.composelearning.sys.ui.sys.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 2:44 PM
 */

data class CommonTopBarConfig(
    val title: String = "ComposeFTP",
    val showBackAction: Boolean = true,
    val showMenuAction: Boolean = true,
    val backAction: () -> Unit = {},
    val action: () -> Unit = {}
)

internal class CommonTopBarConfigProvider : PreviewParameterProvider<CommonTopBarConfig> {
    override val values: Sequence<CommonTopBarConfig> = sequenceOf(
        CommonTopBarConfig(
            title = "ComposeFTP",
            showBackAction = false,
            backAction = {},
            action = {},
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun CommonTopBar(@PreviewParameter(CommonTopBarConfigProvider::class) config: CommonTopBarConfig) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = config.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (config.showBackAction) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            takeIf { config.showMenuAction }?.let {
                IconButton(
                    modifier = Modifier,
                    onClick = config.action
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Menu"
                    )
                }
            }
        },

        )
}