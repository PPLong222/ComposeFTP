package indi.pplong.composelearning.core.file.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.ui.PopupSimpleItem
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiIntent
import indi.pplong.composelearning.core.util.DateUtil
import indi.pplong.composelearning.core.util.FileUtil

/**
 * Description:
 * @author PPLong
 * @date 9/27/24 11:02 AM
 */


@Composable
fun DirAndFileList(
    fileList: List<FileItemInfo>,
    onIntent: (FilePathUiIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .clip(shape = RoundedCornerShape(16.dp))
    ) {
        items(fileList.size) { index ->
            CommonFileItem(
                fileInfo = fileList[index],
                onIntent = onIntent,
                index == fileList.size - 1
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
    isLast: Boolean,
) {

    var isPopVisible by remember { mutableStateOf(false) }
    var isFadeIn by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isFadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 250)
    )
    println(alpha)
    Box {
        Column(
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ListItem(
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
                            stringResource(R.string.time) + DateUtil.getFormatDate(
                                fileInfo.timeStamp,
                                fileInfo.timeStampZoneId
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!fileInfo.isDir) {
                            Text(
                                stringResource(R.string.size) + FileUtil.getFileSize(fileInfo.size),
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                },
                trailingContent = {
                    if (!fileInfo.isDir) {
                        FileTailIconItem(fileInfo, onIntent)
                    }
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
                            if (fileInfo.isDir) {
                                onIntent(
                                    FilePathUiIntent.MoveForward(
                                        fileInfo.pathPrefix
                                            .plus("/")
                                            .plus(fileInfo.name)
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
                                onIntent(FilePathUiIntent.OpenDeleteFileDialog(fileName = fileInfo.name))
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
fun FileTailIconItem(
    fileInfo: FileItemInfo,
    onIntent: (FilePathUiIntent) -> Unit
) {
    val context = LocalContext.current
    var writePermissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        writePermissionState = isGranted
    }

    Log.d("test", "FileTailIconItem: ${fileInfo.transferStatus}")
    when (fileInfo.transferStatus) {
        TransferStatus.Failed, TransferStatus.Initial -> {
            Button(
                onClick = {
                    // TODO: Differentiate by Android Version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        FileUtil.getFileUriInDownloadDir(context, fileInfo.name)?.let { uri ->
                            context.contentResolver.openOutputStream(uri)?.let { stream ->
                                onIntent(
                                    FilePathUiIntent.Download(
                                        stream,
                                        fileInfo,
                                        uri.toString()
                                    )
                                )
                            }
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


