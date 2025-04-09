package indi.pplong.composelearning.core.file.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.core.file.viewmodel.FilePathViewModel

/**
 * Description:
 * @author PPLong
 * @date 4/9/25 10:39 AM
 */

@Composable
fun ControlPanel(viewModel: FilePathViewModel) {
    val downloadCount by viewModel.downloadQueueSize.collectAsState(0)
    val uploadCount by viewModel.uploadQueueSize.collectAsState(0)
    val idleCount by viewModel.idleQueueSize.collectAsState(0)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        NodeItemInfo("Idle", idleCount)
        NodeItemInfo("Download", downloadCount)
        NodeItemInfo("Upload", uploadCount)

    }
}

@Composable
fun NodeItemInfo(
    nodesTitle: String,
    nodesCount: Int
) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(nodesTitle.plus(":"))
        Text(text = nodesCount.toString(), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
@Preview
fun PreviewNodeItemInfo() {
    NodeItemInfo("Download", 10)
}

@Composable
@Preview
fun PreviewControlPanel() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        NodeItemInfo("Idle", 10)
        NodeItemInfo("Download", 10)
        NodeItemInfo("Upload", 10)
    }
}