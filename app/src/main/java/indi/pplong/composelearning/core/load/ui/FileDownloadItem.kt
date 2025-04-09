package indi.pplong.composelearning.core.load.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.core.base.ui.LocalFileAsyncImage
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.util.DateUtil
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.openFileWithUri

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 3:27 PM
 */

@Composable
fun FileDownloadItem(
    fileItemInfo: FileItemInfo
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .height(64.dp)
            .padding(horizontal = 12.dp),

        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Info, null)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text(fileItemInfo.name, style = MaterialTheme.typography.titleSmall)
            if (fileItemInfo.transferStatus is TransferStatus.Transferring) {
                Row {
                    Text(
                        "${FileUtil.getFileSize(fileItemInfo.transferStatus.speed)} /s",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${FileUtil.getFileSize((fileItemInfo.transferStatus.value * fileItemInfo.size).toLong())} / ${
                            FileUtil.getFileSize(
                                fileItemInfo.size
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                LinearProgressIndicator(
                    progress = {
                        fileItemInfo.transferStatus.value
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
@Preview
fun FileTransferredItem(
    transferItemInfo: TransferredFileItem = TransferredFileItem()
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable {
                Uri
                    .parse(transferItemInfo.localUri)
                    .openFileWithUri(context)
            }
    ) {
        ListItem(
            headlineContent = {
                Text(transferItemInfo.remoteName, style = MaterialTheme.typography.titleSmall)
            },
            leadingContent = {
                LocalFileAsyncImage(
                    uri = Uri.parse(transferItemInfo.localUri)
                )
            },
            supportingContent = {
                Row {
                    Text(
                        "Time ${DateUtil.getFormatDate(transferItemInfo.timeMills)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Size ${FileUtil.getFileSize(transferItemInfo.size)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
@Preview
fun PreviewFileLoadItem() {
    FileDownloadItem(
        FileItemInfo(
            name = "File",
            transferStatus = TransferStatus.Transferring(
                0.8F,
                1024 * 1024 * 3
            )
        )
    )
}

@Composable
@Preview
fun PreviewFileLoadedItem() {
    FileTransferredItem(
        TransferredFileItem(
            remoteName = "123",
            remotePathPrefix = "",
            timeMills = 1000L,
            timeZoneId = "1",
            serverHost = "1231",
            transferType = 0,
            size = 1054L,
        )
    )
}