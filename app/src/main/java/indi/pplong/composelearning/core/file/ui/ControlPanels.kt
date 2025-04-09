package indi.pplong.composelearning.core.file.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_hourglass_empty),
                contentDescription = null
            )
            Text(idleCount.toString())
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_upward),
                tint = Color(0xFFFFC107),
                contentDescription = null
            )
            Text(uploadCount.toString())
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_downward),
                tint = Color(0xFF81C784),
                contentDescription = null
            )
            Text(downloadCount.toString())
        }

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(R.drawable.ic_hourglass_empty),
                contentDescription = null
            )
            Text("2")
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_upward),
                tint = Color(0xFFFFF176),
                contentDescription = null
            )
            Text("8")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_downward),
                tint = Color(0xFF81C784),
                contentDescription = null
            )
            Text("10")
        }

    }
}