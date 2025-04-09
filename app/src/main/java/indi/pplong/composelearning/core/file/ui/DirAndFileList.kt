package indi.pplong.composelearning.core.file.ui

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.FileType
import indi.pplong.composelearning.core.base.ui.PopupSimpleItem
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiIntent
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiState
import indi.pplong.composelearning.core.util.DateUtil
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Description:
 * @author PPLong
 * @date 9/27/24 11:02 AM
 */


@Composable
fun DirAndFileList(
    uiState: FilePathUiState,
    onIntent: (FilePathUiIntent) -> Unit
) {
    val state = rememberLazyListState()

    var positionY by remember { mutableStateOf(0) }
    var positionHeightY by remember { mutableStateOf(0) }
    var currentScrollPos by remember { mutableStateOf(0) }
    var moveState by remember { mutableStateOf(0) }
    LaunchedEffect(currentScrollPos) {
        if (currentScrollPos == 0 || positionY == 0) {
            return@LaunchedEffect
        }
        Log.d("ttt", "DirAndFileList: $currentScrollPos $positionY $positionHeightY $moveState")
        if (abs(currentScrollPos - positionY) < 100 && moveState != -1) {
            moveState = -1

        }
        if (abs(positionHeightY - currentScrollPos) < 100 && moveState != 1) {
            moveState = 1
        }
    }

    LaunchedEffect(moveState) {
        while ((moveState == -1) || (moveState == 1)) {
            Log.d("ttt", "scrollToIndexWithConstantSpeed: Running")
            state.scrollBy((if (moveState == -1) -1F else 1F) * 8F) // 按指定速度滚动
            delay(2) // 添加延迟以模拟帧速率，确保滚动平滑
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                positionY = it.positionInRoot().y.toInt()
                positionHeightY = it.positionInRoot().y.toInt() + it.size.height
            },
        state = state
    ) {

        items(uiState.fileList) { item ->
            CommonFileItem(
                fileInfo = item,
                onIntent = onIntent,
                isLast = uiState.fileList.last() == item,
                isOnSelectMode = uiState.appBarStatus == FileSelectStatus.Multiple,
                isSelect = uiState.selectedFileList.contains(item),
                scrollBlock = {
                    // TODO: Consider UiEffect?
                    currentScrollPos = it

                }
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommonFileItem(
    fileInfo: FileItemInfo = FileItemInfo(
        transferStatus = TransferStatus.Loading
    ),
    onIntent: (FilePathUiIntent) -> Unit = {},
    isLast: Boolean = false,
    isOnSelectMode: Boolean = false,
    isSelect: Boolean = false,
    scrollBlock: (Int) -> Unit = {}
) {
    var isPopVisible by remember { mutableStateOf(false) }
    var isFadeIn by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isFadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 250)
    )
    var shouldHighLight by remember { mutableStateOf(false) }

    Column(
        Modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event
                        .mimeTypes()
                        .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = remember {
                    object : DragAndDropTarget {
                        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val text =
                                event.toAndroidDragEvent().clipData?.getItemAt(0)?.text
                                    ?: return true
                            Log.d(
                                "intent",
                                "onDrop: $text"
                            )
                            if (fileInfo.fullPath != text && fileInfo.isDir) {
                                val targetPath = fileInfo.fullPath + "/" + text
                                    .split("/")
                                    .last()
                                onIntent(
                                    FilePathUiIntent.Browser.MoveFile(
                                        text.toString(),
                                        targetPath
                                    )
                                )
                            }
                            return true
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            super.onEnded(event)
                            shouldHighLight = false
                        }

                        override fun onMoved(event: DragAndDropEvent) {
                            super.onMoved(event)

                            scrollBlock(event.toAndroidDragEvent().y.toInt())
                        }

                        override fun onEntered(event: DragAndDropEvent) {
                            super.onEntered(event)
                            val filePath = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text
                            if (fileInfo.fullPath != filePath && fileInfo.isDir) {
                                shouldHighLight = true
                            }
                        }

                        override fun onExited(event: DragAndDropEvent) {
                            super.onExited(event)
                            shouldHighLight = false
                        }
                    }
                }
            )
            .dragAndDropSource {
                detectTapGestures(
                    onLongPress = {
                        startTransfer(
                            transferData = DragAndDropTransferData(
                                clipData = ClipData.newPlainText(
                                    "DragData", fileInfo.fullPath
                                )
                            )
                        )
                    },
                    onTap = {
                        if (!isOnSelectMode) {
                            if (fileInfo.isDir) {
                                onIntent(
                                    FilePathUiIntent.Browser.MoveForward(
                                        fileInfo.pathPrefix
                                            .plus("/")
                                            .plus(fileInfo.name)
                                    )
                                )
                            }
                        } else {
                            onIntent(
                                FilePathUiIntent.Browser.OnFileSelect(
                                    fileInfo
                                )
                            )
                        }
                    }
                )
            }
    ) {
        CommonListItem(
            leadingContent = {
                DirAndFileIcon(
                    cache = {
                        onIntent(
                            FilePathUiIntent.Browser.CacheItem(fileInfo)
                        )
                    },
                    fileInfo = fileInfo
                )
            },
            headlineContent = {
                Text(
                    fileInfo.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.widthIn(max = 240.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Row {
                    Text(
                        DateUtil.getFormatDate(
                            fileInfo.timeStamp,
                            fileInfo.timeStampZoneId
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!fileInfo.isDir) {
                        Text(
                            FileUtil.getFileSize(fileInfo.size),
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

            },
            trailingContent = {
                FileTailIconItem(
                    fileInfo,
                    onIntent,
                    isOnSelectMode = isOnSelectMode,
                    isSelect = isSelect
                )
            },
            backgroundColor = MaterialTheme.colorScheme.surface
        )


        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    if (isPopVisible || alpha > 0F) {
        Popup(
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true
            ),
            onDismissRequest = {
                isFadeIn = false
            },
            offset = IntOffset(100, 100)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .graphicsLayer(alpha = alpha),

                ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    PopupSimpleItem(
                        text = "Delete",
                        imageVector = Icons.Default.Delete,
                        onclick = {
                            isPopVisible = false
                            isFadeIn = false
                        })
                    PopupSimpleItem(text = "Rename", imageVector = Icons.Default.Edit)
                    PopupSimpleItem(
                        text = "Move",
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight
                    )
                }

            }

            LaunchedEffect(alpha) {
                if (alpha == 0f) {
                    isPopVisible = false
                }
            }
        }
    }
}

@Composable
@Preview
fun DirAndFileIcon(
    cache: () -> Unit = { },
    fileInfo: FileItemInfo = FileItemInfo()
) {
    val fileType = FileUtil.getFileType(fileInfo.name)
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {

        if (fileInfo.isDir) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            when (fileType) {
                FileType.PNG -> {
                    // File Size > 100MB, show default image icon
                    if (fileInfo.size > 1024 * 1024 * 100) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        FileThumbnailAsyncImage(
                            key = fileInfo.md5,
                            localUri = fileInfo.localUri,
                            cache = cache
                        )
                    }

                }

                FileType.VIDEO -> {
                    FileThumbnailAsyncImage(
                        key = fileInfo.md5,
                        localUri = fileInfo.localUri,
                        cache = cache
                    )
                }

                FileType.MUSIC -> {}
                FileType.OTHER -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_description),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun FileTailIconItem(
    fileInfo: FileItemInfo,
    onIntent: (FilePathUiIntent) -> Unit,
    isOnSelectMode: Boolean = true,
    isSelect: Boolean = true,

    ) {
    if (fileInfo.isDir) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp)
        )
        return
    }

    if (isOnSelectMode) {
        Checkbox(
            checked = isSelect,
            onCheckedChange = null
        )
    } else {
        when (fileInfo.transferStatus) {
            TransferStatus.Failed, TransferStatus.Initial -> {
//                Button(
//                    onClick = {
//                        // TODO: Differentiate by Android Version
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                            FileUtil.getFileUriInDownloadDir(context, fileInfo.name)?.let { uri ->
//                                onIntent(
//                                    FilePathUiIntent.Browser.Download(
//                                        fileInfo,
//                                        uri.toString()
//                                    )
//                                )
//                            }
//
//
//                        } else {
//                            // TODO
//                        }
//                    },
//                    modifier = Modifier
//                        .width(32.dp)
//                        .height(32.dp),
//                    shape = RoundedCornerShape(8.dp),
//                    contentPadding = PaddingValues(4.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.primary
//                    )
//                ) {
//                    Icon(
//                        Icons.Default.KeyboardArrowDown,
//                        null
//                    )
//                }
            }

            TransferStatus.Successful -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            is TransferStatus.Transferring -> {
                CircularProgressIndicator(
                    progress = { fileInfo.transferStatus.value },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(32.dp)
                )
            }

            TransferStatus.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(32.dp)
                )
            }
        }
    }


}

@Composable
fun CommonListItem(
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    // TODO: Standardized
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 80.dp)
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.invoke() ?: Box(modifier = Modifier)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1F)
        ) {
            headlineContent.invoke()
            Spacer(Modifier.height(4.dp))
            supportingContent?.invoke()
        }
        Spacer(Modifier.width(4.dp))
        trailingContent?.invoke()

    }
}

@Composable
@Preview
fun PreviewCommonListItem(modifier: Modifier = Modifier) {
    ComposeLearningTheme {
        CommonListItem(
            headlineContent = {}
        )
    }
}

@Composable
@Preview
fun PreviewCommonFileItem(modifier: Modifier = Modifier) {
    ComposeLearningTheme {
        CommonFileItem()
    }
}