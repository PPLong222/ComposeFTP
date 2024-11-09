package indi.pplong.composelearning.core.host.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.viewmodel.ServerUiIntent
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 7:05 PM
 */
data class ConnectedHostBean(
    val ip: String,
    val user: String,
    val password: String,
    val lastConnectedTime: String
)


@Composable
fun ConnectedHost(
    serverItemInfo: ServerItemInfo = ServerItemInfo(),
    onIntent: (ServerUiIntent) -> Unit = {},
    isConnecting: Boolean = false
) {

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(imageVector = Icons.Default.Share, null)
            },
            headlineContent = {
                Text(serverItemInfo.nickname, style = MaterialTheme.typography.titleMedium)
            },
            overlineContent = {},
            supportingContent = {
                Column {
                    Text(
                        "User: ${serverItemInfo.user}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

            },
            trailingContent = {
                if (!isConnecting) {
                    Button(
                        onClick = {
                            onIntent(ServerUiIntent.ConnectServer(serverItemInfo))
                        },
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .width(48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, null)
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        )
    }
}

@Preview
@Composable
fun ConnectedHostPreview() {
    ComposeLearningTheme {
        ConnectedHost()
    }
}