package indi.pplong.composelearning.core.load.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.load.viewmodel.TransferUiIntent
import indi.pplong.composelearning.core.load.viewmodel.TransferViewModel

/**
 * Description:
 * @author PPLong
 * @date 10/24/24 8:37 PM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBottomSheet(
    onDismissRequest: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        TransferScreen()
    }
}

@Preview
@Composable
fun TransferScreen() {
    val viewModel = hiltViewModel<TransferViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        TwinTab(
            selectIndex = uiState.curIndex,
            activatedColor = MaterialTheme.colorScheme.surfaceDim,
            deactivatedColor = MaterialTheme.colorScheme.surfaceContainer,
            onTabClick = { viewModel.sendIntent(TransferUiIntent.SwitchTab(it)) }
        )
        if (uiState.curIndex == 0) {
            DownloadList(uiState.downloadFileList, uiState.alreadyDownloadFileList)
        } else {
            UploadList(uiState.uploadFileList, uiState.alreadyUploadFileList, viewModel::sendIntent)
        }
    }

}

@Composable
fun DownloadList(
    downloadFileList: List<FileItemInfo>,
    alreadyDownloadedList: List<TransferredFileItem>
) {
    LazyColumn {
        item {
            TransferHeadText("Downloading")
        }
        items(downloadFileList) { fileItemInfo ->
            FileDownloadItem(fileItemInfo)
        }
        item {
            TransferHeadText("History")
        }
        items(alreadyDownloadedList) { transferredFileItem ->
            FileTransferredItem(transferredFileItem)
        }
    }
}

@Composable
fun UploadList(
    uploadFileList: List<TransferringFile>,
    alreadyUploadedList: List<TransferredFileItem>,
    onIntent: (TransferUiIntent) -> Unit = {}
) {
    LazyColumn {
        item {
            TransferHeadText("Uploading")
        }
        items(uploadFileList) { fileItemInfo ->
            FileUploadItem(fileItemInfo)
        }
        item {
            TransferHeadText("History")
        }
        items(alreadyUploadedList) { transferredFileItem ->
            FileTransferredItem(transferredFileItem, onIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabHead(curIndex: Int, onTabClicked: (Int) -> Unit) {

    val tabList = listOf("Download", "Upload")
    val tabIconList = listOf(Icons.Default.KeyboardArrowDown, Icons.Default.KeyboardArrowUp)
    SecondaryTabRow(
        curIndex, modifier = Modifier
            .wrapContentWidth()
    ) {
        tabList.forEachIndexed { index, tab ->
            Tab(
                selected = curIndex == index,
                onClick = { onTabClicked(index) },
                modifier = Modifier
                    .background(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ),
                text = {
                    Text(
                        tabList[index], style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }, icon = {
                    Icon(imageVector = tabIconList[index], contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun TransferHeadText(text: String = "") {
    Text(
        text,
        modifier = Modifier.padding(12.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}
