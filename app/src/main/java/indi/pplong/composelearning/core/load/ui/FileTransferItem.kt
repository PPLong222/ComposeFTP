package indi.pplong.composelearning.core.load.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.ui.LocalFileAsyncImage
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.file.ui.CommonListItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.load.viewmodel.TransferUiIntent
import indi.pplong.composelearning.core.util.DateUtil
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.openFileWithUri

/**
 * Description:
 * @author PPLong
 * @date 9/29/24 3:27 PM
 */

@Composable
fun FileDownloadingItem(
    fileItemInfo: TransferringFile
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_description), null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.padding(end = 16.dp)) {
            Text(
                fileItemInfo.transferredFileItem.remoteName,
                style = MaterialTheme.typography.titleSmall
            )
            if (fileItemInfo.transferStatus is TransferStatus.Transferring) {
                Row {
                    Text(
                        "${FileUtil.getFileSize(fileItemInfo.transferStatus.speed)} /s",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${FileUtil.getFileSize((fileItemInfo.transferStatus.value * fileItemInfo.transferredFileItem.size).toLong())} / ${
                            FileUtil.getFileSize(
                                fileItemInfo.transferredFileItem.size
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
fun FileUploadingItem(
    transferringFile: TransferringFile
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalFileAsyncImage(
            transferringFile.transferredFileItem.localImageUri.toUri()
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier) {
            Text(
                transferringFile.transferredFileItem.remoteName,
                style = MaterialTheme.typography.titleSmall
            )
            if (transferringFile.transferStatus is TransferStatus.Transferring) {
                Text(
                    "Download Speed ${FileUtil.getFileSize(transferringFile.transferStatus.speed)} /s",
                    style = MaterialTheme.typography.labelSmall
                )
                LinearProgressIndicator(
                    progress = {
                        transferringFile.transferStatus.value
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp, end = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
@Preview
fun FileTransferredItem(
    transferItemInfo: TransferredFileItem = TransferredFileItem(),
    onIntent: (TransferUiIntent) -> Unit = {}
) {
    val context = LocalContext.current
    val uri = transferItemInfo.localImageUri.toUri()
    LaunchedEffect(Unit) {
        if (!FileUtil.isFileProviderUriExists(
                context,
                uri
            )
        ) {
            onIntent(TransferUiIntent.CacheImage(transferItemInfo, context))
        }
    }

    Column(
        modifier = Modifier
            .clickable {
                transferItemInfo.localUri.toUri()
                    .openFileWithUri(context)
            }
    ) {
        CommonListItem(
            headlineContent = {
                Text(transferItemInfo.remoteName, style = MaterialTheme.typography.titleSmall)
            },
            leadingContent = {
                LocalFileAsyncImage(
                    uri = transferItemInfo.localImageUri.toUri()
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
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
@Preview
fun PreviewFileLoadItem() {
    FileDownloadingItem(
        TransferringFile()
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