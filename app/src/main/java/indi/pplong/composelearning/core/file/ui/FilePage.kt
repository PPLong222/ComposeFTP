package indi.pplong.composelearning.core.file.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiEffect
import indi.pplong.composelearning.core.file.viewmodel.FilePathUiIntent
import indi.pplong.composelearning.core.file.viewmodel.FilePathViewModel
import indi.pplong.composelearning.core.load.ui.TransferBottomSheet
import indi.pplong.composelearning.core.load.ui.TransferForegroundService
import indi.pplong.composelearning.core.util.PermissionUtils
import indi.pplong.composelearning.sys.ui.sys.widgets.CommonTopBar
import kotlinx.coroutines.launch

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 11:51 AM
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun BrowsePage(
    navController: NavController = rememberNavController(),
    nickname: String = "",
    hostKey: Long = 0L,
) {
    val viewModel: FilePathViewModel =
        hiltViewModel<FilePathViewModel, FilePathViewModel.FilePathViewModelFactory>(
            creationCallback = { factory ->
                factory.create(hostKey)
            }
        )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renamedOriginalFileName by remember { mutableStateOf("") }
    val context = LocalContext.current
    var binder by remember { mutableStateOf<TransferForegroundService.TransferBinder?>(null) }
    val transferServiceConn = remember {
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                Log.d("123123", "onServiceConnected: service conttect")
                binder = service as? TransferForegroundService.TransferBinder
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
            }

        }
    }

    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // 用户选择目录后的处理逻辑
        if (uri != null) {
            viewModel.sendIntent(
                FilePathUiIntent.AppBar.Upload(uri, uiState.path)
            )

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (uiState.showTransferSheet) {
        TransferBottomSheet(
            onDismissRequest = {
                viewModel.sendIntent(
                    FilePathUiIntent.AppBar.SetTransferSheetShow(
                        false
                    )
                )
            }
        )
    }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FilePathUiEffect.OnDeleteFile -> {
                    showDeleteDialog = false
                    viewModel.sendIntent(FilePathUiIntent.AppBar.Refresh)
                }

                is FilePathUiEffect.ShowDeleteDialog -> {
                    showDeleteDialog = true
                }

                FilePathUiEffect.DismissDeleteDialog -> {
                    showDeleteDialog = false
                    showRenameDialog = false
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

                is FilePathUiEffect.ShowSnackBar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            effect.message,
                            effect.actionLabel,
                            effect.withDismissAction,
                            effect.duration
                        )
                    }
                }

                is FilePathUiEffect.ShowFileRenameDialog -> {
                    showRenameDialog = true
                    renamedOriginalFileName = effect.name
                }

                is FilePathUiEffect.LaunchTransferService -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        binder?.let {
                            binder?.addTransferTask(
                                hostKey,
                                effect.downloadFileList,
                                effect.uploadFileList
                            )

                        } ?: run {
                            startTransferService(
                                context,
                                transferServiceConn,
                                hostKey,
                                effect.downloadFileList,
                                effect.uploadFileList
                            )
                        }
                    }
                }
            }
        }
    }

    val transferringCount by viewModel.transferringCount.collectAsState(0)

    Scaffold(
        topBar = {
            CommonTopBar(
                hasSelect = uiState.appBarStatus == FileSelectStatus.Multiple,
                host = nickname,
                onClickSelect = {
                    viewModel.sendIntent(
                        FilePathUiIntent.AppBar.SelectFileMode(
                            uiState.appBarStatus == FileSelectStatus.Single
                        )
                    )
                },
                onTransferClick = {
                    viewModel.sendIntent(FilePathUiIntent.AppBar.SetTransferSheetShow(true))
                },
                transferredCount = transferringCount,
                isTransferStatusViewed = uiState.isTransferStatusViewed,
            )
        },
        bottomBar = {
            FileActionBottomAppBar(
                barStatus = uiState.appBarStatus,
                onClickFAB = viewModel::bottomAppBarFABActionEvents,
                events = viewModel::bottomAppBarActionEvents,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        if (showDeleteDialog) {
            DeleteFileConfirmDialog(
                onConfirmed = { viewModel.sendIntent(FilePathUiIntent.Dialog.DeleteFile) },
                onCancel = { viewModel.sendIntent(FilePathUiIntent.Dialog.DismissAllDialog) }
            )
        }
        if (uiState.createDirDialog.isShow) {
            CreateDirDialog(
                loadingState = uiState.createDirDialog.loadingStatus,
                onConfirmed = { viewModel.sendIntent(FilePathUiIntent.Dialog.CreateDirectory(it)) },
                onCancel = { viewModel.sendIntent(FilePathUiIntent.Dialog.DismissAllDialog) }
            )
        }

        if (showRenameDialog) {
            RenameFileDialog(
                onConfirmed = { old, new ->
                    viewModel.sendIntent(
                        FilePathUiIntent.Dialog.RenameFile(
                            old,
                            new
                        )
                    )
                },
                onCancel = { viewModel.sendIntent(FilePathUiIntent.Dialog.DismissAllDialog) },
                originalName = renamedOriginalFileName
            )
        }

        Column(
            Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            ControlPanel(viewModel)
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
                        .padding(horizontal = 16.dp)
                ) {
                    HeadPathNavigation(
                        uiState.path
                    ) {
                        viewModel.sendIntent(
                            FilePathUiIntent.Browser.MoveForward(
                                it
                            )
                        )
                    }
                }
                FileSortTypeMenu(
                    uiState.fileSortMode,
                    onChange = {
                        viewModel.sendIntent(FilePathUiIntent.Browser.OnFileSortModeChange(it))
                    }
                )
            }
            if (uiState.loadingState == LoadingState.LOADING) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text("Loading...", style = MaterialTheme.typography.headlineMedium)
                    }

                }
            } else {
                if (uiState.fileList.isEmpty()) {
                    EmptyFolderTip()
                } else {
                    // Body
                    DirAndFileList(uiState, viewModel::sendIntent)
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
fun EmptyFolderTip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(R.drawable.ic_file_open),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
        Text(
            "Empty Folder",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}


fun startTransferService(
    context: Context, connection: ServiceConnection,
    hostKey: Long,
    downloadList: List<CommonFileInfo>, uploadFileList: List<CommonFileInfo>
) {
    Intent(context, TransferForegroundService::class.java).apply {
        putExtra("host_key", hostKey)
        putParcelableArrayListExtra("download_list", ArrayList(downloadList))
        putParcelableArrayListExtra("upload_list", ArrayList(uploadFileList))

        context.startForegroundService(this)
        context.bindService(this, connection, Context.BIND_AUTO_CREATE)

    }

}