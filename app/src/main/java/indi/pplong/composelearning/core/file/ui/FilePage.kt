package indi.pplong.composelearning.core.file.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.file.model.FileActionBottomAppBarStatus
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiEffect
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiIntent
import indi.pplong.composelearning.core.file.viewmodel.FilePathViewModel
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.PermissionUtils
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonTopBar

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 11:51 AM
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsePage(
    host: String = "",
) {
    val viewModel: FilePathViewModel =
        hiltViewModel<FilePathViewModel, FilePathViewModel.FilePathViewModelFactory>(
            creationCallback = { factory ->
                factory.create(host)
            }
        )
    Log.d("FilePage", "FilePage: Init ViewModel")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // 用户选择目录后的处理逻辑
        if (uri != null) {
            val fileSize = FileUtil.getFileSize(context, uri)
            val fileName = FileUtil.getFileName(context, uri)
            context.contentResolver.openInputStream(uri)?.let { stream ->
                viewModel.sendIntent(
                    FilePathUiIntent.Upload(
                        TransferringFile(
                            transferredFileItem = TransferredFileItem(
                                remoteName = fileName,
                                remotePathPrefix = uiState.path,
                                size = fileSize,
                                transferType = 1,
                                localUri = uri.toString()
                            )
                        ), stream
                    )
                )
            }
            // Make this uri read permission persistable
            PermissionUtils.takePersistableUriPermission(context.contentResolver, uri)
        }
    }

    // Launcher to request permissions for Android 13 and above
    val requestMediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle the permissions result
        val readMediaImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val readMediaVideoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        if (readMediaImagesGranted && readMediaVideoGranted) {
            openDirectoryLauncher.launch(arrayOf("*/*"))
        } else {
            // Permissions denied
        }
    }

    // Launcher to request permissions for Android 12 and below
    val requestExternalStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FilePathUiEffect.OnDeleteFile -> {
                    showDialog = false
                    viewModel.sendIntent(FilePathUiIntent.Refresh)
                }

                is FilePathUiEffect.ShowDeleteDialog -> {
                    showDialog = true
                }

                FilePathUiEffect.DismissDeleteDialog -> {
                    showDialog = false
                }

                FilePathUiEffect.ShowFileSelectWindow -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13 and above
                        requestMediaPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                            )
                        )
                    } else {
                        // Android 12 and below
                        requestExternalStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }
    val scrollState = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            CommonTopBar(
                hasSelect = uiState.appBarStatus == FileActionBottomAppBarStatus.FILE,
                onClickSelect = {
                    viewModel.sendIntent(
                        FilePathUiIntent.AppBar.SelectFileMode(
                            uiState.appBarStatus == FileActionBottomAppBarStatus.DIRECTORY
                        )
                    )
                }
            )
        },
        bottomBar = {
            FileActionBottomAppBar(
                barStatus = uiState.appBarStatus,
                onClickFAB = viewModel::bottomAppBarFABActionEvents,
                events = viewModel::bottomAppBarActionEvents,
                scrollBehavior = scrollState
            )
        },
    ) { paddingValues ->
        if (showDialog) {
            DeleteFileConfirmDialog(
                onConfirmed = { viewModel.sendIntent(FilePathUiIntent.DeleteFile) },
                onCancel = { viewModel.sendIntent(FilePathUiIntent.DismissDialog) }
            )
        }
        if (uiState.activeList.isEmpty()) {
            EmptyConnectionTip()
        } else {
            Column(
                Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(paddingValues)
                    .fillMaxWidth()
            ) {
                // Ban Multi FTP Window
//                LazyRow {
//                    items(uiState.activeList) { server ->
//                        ServerChip(label = server.serverHost)
//                    }
//                }
                // Head
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1F)
                            .padding(horizontal = 8.dp)
                    ) {
                        HeadPathNavigation(
                            uiState.path
                        ) {
                            viewModel.sendIntent(
                                FilePathUiIntent.MoveForward(
                                    it
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (uiState.loadingState == LoadingState.LOADING) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 6.dp
                            )
                            Text("Loading...", style = MaterialTheme.typography.headlineLarge)
                        }

                    }
                } else {
                    // Body
                    DirAndFileList(uiState, viewModel::sendIntent, scrollState)
                }

            }
        }
    }
}

@Composable
@Preview
fun UploadButton(
    upload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Button(
        onClick = upload,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.heightIn(28.dp)
    ) {
        Text(
            "Upload", color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
@Preview
fun EmptyConnectionTip(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_connection),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.nothing_is_here),
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Composable
@Preview
fun DeleteFileConfirmDialog(
    modifier: Modifier = Modifier,
    onConfirmed: () -> Unit = {},
    onCancel: () -> Unit = {},

    ) {
    var isDeleting by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {
            println("123")
        },
        confirmButton = {
            if (!isDeleting) {
                Button(onClick = {
                    isDeleting = true
                    onConfirmed()
                }) {
                    Text("DELETE")
                }
            }

        },
        dismissButton = {
            if (!isDeleting) {
                Button(onClick = onCancel) {
                    Text("Back")
                }
            }
        },
        icon = {
            Icon(Icons.Default.Info, contentDescription = null)
        },
        title = {
            Text("Delete Confirmation")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Are you sure you are going to delete File?")
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp)
                    )
                }
            }


        }
    )
}