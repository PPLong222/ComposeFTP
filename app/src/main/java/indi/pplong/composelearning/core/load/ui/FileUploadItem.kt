package indi.pplong.composelearning.core.load.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.core.base.ui.LocalFileAsyncImage
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.FileUtil

/**
 * Description:
 * @author PPLong
 * @date 10/25/24 10:06 PM
 */
@Composable
fun FileUploadItem(
    transferringFile: TransferringFile,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .height(64.dp)
            .padding(horizontal = 12.dp),

        verticalAlignment = Alignment.CenterVertically,

        ) {
        LocalFileAsyncImage(
            Uri.parse(transferringFile.transferredFileItem.localUri)
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