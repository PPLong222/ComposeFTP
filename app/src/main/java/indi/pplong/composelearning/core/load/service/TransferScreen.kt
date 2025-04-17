package indi.pplong.composelearning.core.load.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.load.ui.FileDownloadingItem
import indi.pplong.composelearning.core.load.ui.FileTransferredItem
import indi.pplong.composelearning.core.load.ui.FileUploadingItem
import indi.pplong.composelearning.core.load.viewmodel.TransferUiIntent
import indi.pplong.composelearning.core.load.viewmodel.TransferViewModel
import kotlinx.coroutines.launch

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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
        }
    ) {
        TransferScreen()
    }
}

@Preview
@Composable
fun TransferScreen() {
    val viewModel = hiltViewModel<TransferViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        pageCount = { 2 },
        initialPage = 0
    )
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            Tab(selected = pagerState.currentPage == 0, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }) {
                Text(
                    "Download",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            Tab(selected = pagerState.currentPage == 1, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }) {
                Text(
                    "Upload",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxHeight(),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top
        ) { pageIdx ->
            if (pageIdx == 0) {
                DownloadList(uiState.downloadFileList, uiState.alreadyDownloadFileList)
            } else {
                UploadList(
                    uiState.uploadFileList,
                    uiState.alreadyUploadFileList,
                    viewModel::sendIntent
                )
            }
        }
//        TwinTab(
//            selectIndex = uiState.curIndex,
//            activatedColor = MaterialTheme.colorScheme.surfaceDim,
//            deactivatedColor = MaterialTheme.colorScheme.surfaceContainer,
//            onTabClick = { viewModel.sendIntent(TransferUiIntent.SwitchTab(it)) }
//        )
//        if (uiState.curIndex == 0) {
//            DownloadList(uiState.downloadFileList, uiState.alreadyDownloadFileList)
//        } else {
//            UploadList(uiState.uploadFileList, uiState.alreadyUploadFileList, viewModel::sendIntent)
//        }
    }

}

@Composable
fun DownloadList(
    downloadFileList: List<TransferringFile>,
    alreadyDownloadedList: List<TransferredFileItem>
) {

    LazyColumn {
        item {
            TransferHeadText("Downloading")
        }
        items(downloadFileList) { fileItemInfo ->
            FileDownloadingItem(fileItemInfo)
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
            FileUploadingItem(fileItemInfo)
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
