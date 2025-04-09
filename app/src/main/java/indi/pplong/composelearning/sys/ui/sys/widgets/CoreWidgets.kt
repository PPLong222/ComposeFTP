package indi.pplong.composelearning.sys.ui.sys.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 2:44 PM
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun CommonTopBar(
    host: String = "192.168.1.1",
    hasSelect: Boolean = false,
    isTransferStatusViewed: Boolean = true,
    transferredCount: Int = 0,
    onClickSelect: () -> Unit = {},
    onTransferClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = host,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )

            }
        },
        actions = {
            BadgedBox(
                badge = {
                    if (transferredCount > 0) {
                        Badge {
                            Text(transferredCount.toString())
                        }
                    } else if (!isTransferStatusViewed) {
                        Badge()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_swap_vert), contentDescription = null,
                    modifier = Modifier.clickable { onTransferClick() }
                )
            }

            Spacer(Modifier.width(12.dp))
            AnimatedContent(targetState = hasSelect) { state ->
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable {
                            onClickSelect()
                        }) {
                    if (state) {
                        Text("Done", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("Select", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )

    )
}