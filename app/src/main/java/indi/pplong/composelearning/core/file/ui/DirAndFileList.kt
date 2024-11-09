package indi.pplong.composelearning.core.file.ui

import android.os.Build
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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

/**
 * Description:
 * @author PPLong
 * @date 9/27/24 11:02 AM
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirAndFileList(
    uiState: FilePathUiState,
    onIntent: (FilePathUiIntent) -> Unit,
    scrollBehavior: BottomAppBarScrollBehavior
) {
    LazyColumn(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        items(uiState.fileList) { item ->
            CommonFileItem(
                fileInfo = item,
                onIntent = onIntent,
                uiState.fileList.last() == item,
                isOnSelectMode = uiState.appBarStatus == FileSelectStatus.Multiple,
                isSelect = (uiState.selectedFileList.contains(item))

            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun CommonFileItem(
    fileInfo: FileItemInfo = FileItemInfo(
        transferStatus = TransferStatus.Loading
    ),
    onIntent: (FilePathUiIntent) -> Unit = {},
    isLast: Boolean = false,
    isOnSelectMode: Boolean = false,
    isSelect: Boolean = false,
) {

    var isPopVisible by remember { mutableStateOf(false) }
    var isFadeIn by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isFadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 250)
    )
    Box {
        Column(
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ListItem(
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
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),

                modifier = Modifier
                    .combinedClickable(
                        onLongClick = {
                            isPopVisible = true
                            isFadeIn = true
                        },
                        onClick = {
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
                                        fileInfo,
                                        !isSelect
                                    )
                                )
                            }

                        }
                    )
            )
            if (!isLast) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
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
}

@Composable
@Preview
fun DirAndFileIcon(
    cache: () -> Unit = { },
    fileInfo: FileItemInfo = FileItemInfo()
) {
    val fileType = FileUtil.getFileType(fileInfo.name)
    val context = LocalContext.current
    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {

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
                    Log.d("ttt", "DirAndFileIcon: md5 ${fileInfo.md5}")
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
            // Video
//            Image(
//                bitmap = FileUtil.getVideoThumbnailWithRetriever(
//                    contentResolver = context.contentResolver,
//                    videoUri = Uri.parse("content://media/external/downloads/1000018392"),
//                    width = 48,
//                    height = 48
//                )!!.asImageBitmap(),
//                contentDescription = null,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .clip(RoundedCornerShape(8.dp))
//            )
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
    val context = LocalContext.current

    if (isOnSelectMode) {
        Checkbox(
            checked = isSelect,
            onCheckedChange = null
        )
    } else {
        Log.d("test", "FileTailIconItem: ${fileInfo.transferStatus}")
        when (fileInfo.transferStatus) {
            TransferStatus.Failed, TransferStatus.Initial -> {
                Button(
                    onClick = {
                        // TODO: Differentiate by Android Version
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtil.getFileUriInDownloadDir(context, fileInfo.name)?.let { uri ->
                                onIntent(
                                    FilePathUiIntent.Browser.Download(
                                        fileInfo,
                                        uri.toString()
                                    )
                                )
                            }


                        } else {
                            // TODO
                        }
                    },
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null
                    )
                }
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




