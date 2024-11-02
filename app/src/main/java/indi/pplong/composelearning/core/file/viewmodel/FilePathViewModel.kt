package indi.pplong.composelearning.core.file.viewmodel

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.toMutableStateList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.cache.FTPClientCache
import indi.pplong.composelearning.core.cache.FTPServerPool
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.model.toFileItemInfo
import indi.pplong.composelearning.core.file.ui.FileBottomAppBarAction
import indi.pplong.composelearning.core.load.model.TransferringFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:16 PM
 */
@HiltViewModel(assistedFactory = FilePathViewModel.FilePathViewModelFactory::class)
class FilePathViewModel @AssistedInject constructor(
    @Assisted val host: String
) :
    BaseViewModel<FilePathUiState, FilePathUiIntent, FilePathUiEffect>() {
    private val TAG = javaClass.name
    private lateinit var cache: FTPClientCache

    init {
        Log.d(TAG, "Init ViewModfel: $host")
        launchOnIO {
            FTPServerPool.serverFTPMap.collect {
                launch {
                    it.values.firstOrNull { it.coreFTPClient.host == host }?.let {
                        cache = it
                        getCurrentListFiles()
                        Log.d(TAG, ": ${it.coreFTPClient.host}")
                        it
                    }?.downloadQueue?.collect { queue ->
                        Log.d(TAG, "$queue: download Get")
                        queue.forEach { client ->
                            launch {
                                Log.d(TAG, "Download Queue Update ${client.toString()}")
                                client.transferFileFlow.collect { file ->
                                    val index =
                                        uiState.value.fileList.indexOfLast { it.pathPrefix == file.pathPrefix && it.name == file.name }
                                    Log.d(TAG, "Change File True")
                                    takeIf { index >= 0 }?.let {
                                        setState {
                                            copy(fileList = fileList.toMutableList().apply {
                                                set(index, file)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    override fun initialState(): FilePathUiState {
        return FilePathUiState()
    }


    override suspend fun handleIntent(intent: FilePathUiIntent) {
        when (intent) {
            is FilePathUiIntent.Browser -> handleBrowserIntent(intent)

            is FilePathUiIntent.AppBar -> handleAppBarIntent(intent)

            is FilePathUiIntent.Dialog -> handleDialogIntent(intent)

            is FilePathUiIntent.SnackBarHost -> handleSnackBarIntent(intent)
        }
    }

    private fun handleBrowserIntent(intent: FilePathUiIntent.Browser) {
        when (intent) {
            is FilePathUiIntent.Browser.MoveForward -> onPathChanged(intent.path)
            is FilePathUiIntent.Browser.Download -> {
                downloadSingleFile(intent.outputStream, intent.fileItemInfo, intent.localUri)
            }

            is FilePathUiIntent.Browser.OnFileSelect -> onFileSelected(
                intent.fileName,
                intent.select,
                intent.isDir
            )
        }
    }

    private fun handleAppBarIntent(intent: FilePathUiIntent.AppBar) {
        when (intent) {
            is FilePathUiIntent.AppBar.Upload -> {
                uploadSingleFile(intent.transferringFile, intent.inputStream)
            }

            is FilePathUiIntent.AppBar.SelectFileMode -> {
                onSelectModeChanged(intent.select)
            }

            is FilePathUiIntent.AppBar.OpenDeleteFileDialog -> {
                sendEffect {
                    FilePathUiEffect.ShowDeleteDialog(intent.fileName)
                }
            }

            FilePathUiIntent.AppBar.OpenFileSelectWindow -> {
                sendEffect { FilePathUiEffect.ShowFileSelectWindow }
            }

            is FilePathUiIntent.AppBar.DownloadMultipleFiles -> {}


            FilePathUiIntent.AppBar.Refresh -> refresh()
            FilePathUiIntent.AppBar.ClickCreateDirIcon -> {
                setState { copy(createDirDialog = createDirDialog.copy(isShow = true)) }
            }
        }
    }

    private fun handleDialogIntent(intent: FilePathUiIntent.Dialog) {
        when (intent) {
            FilePathUiIntent.Dialog.DeleteFile -> deleteFile()
            FilePathUiIntent.Dialog.DismissDialog -> {
                // TODO: Unify to UiState
                sendEffect {
                    FilePathUiEffect.DismissDeleteDialog
                }
                handleDismissDialog()
            }

            is FilePathUiIntent.Dialog.CreateDirectory -> {
                createDirectory(intent.fileName)
            }
        }
    }

    private fun handleSnackBarIntent(intent: FilePathUiIntent.SnackBarHost) {
        when (intent) {
            FilePathUiIntent.SnackBarHost.PerformClick -> {}
        }
    }

    private fun deleteFile() {
        launchOnIO {
            val fileSize = uiState.value.selectedFileList.size
            val deleteFileRes =
                cache.coreFTPClient.deleteFile(uiState.value.selectedFileList.toList())
            val deleteDirRes =
                cache.coreFTPClient.deleteDirectory(uiState.value.selectDirList.toList())
            delay(2000)
            if (deleteFileRes && deleteDirRes) {
                setState {
                    copy(
                        selectedFileList = setOf(),
                        appBarStatus = FileSelectStatus.Single
                    )
                }
                sendEffect { FilePathUiEffect.OnDeleteFile }
                sendEffect {
                    FilePathUiEffect.ShowSnackBar(
                        message = "You delete ${fileSize} file",
                        actionLabel = "Undo",
                        withDismissAction = false,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

    }

    private fun downloadSingleFile(
        outputStream: OutputStream,
        fileInfo: FileItemInfo,
        localUri: String
    ) {
        val newFileInfo =
            fileInfo.copy(transferStatus = TransferStatus.Loading, localUri = localUri)
        setState {
            copy(fileList = fileList.toMutableStateList().apply {
                set(indexOf(fileInfo), newFileInfo)
            })
        }
        launchOnIO {
            cache.downloadFile(outputStream, newFileInfo)
        }
    }

    private fun uploadSingleFile(transferringFile: TransferringFile, inputStream: InputStream) {
        val file = transferringFile.copy(
            transferredFileItem = transferringFile.transferredFileItem.copy(serverHost = cache.coreFTPClient.host)
        )

        launchOnIO {
            cache.uploadFile(file, inputStream)
        }
    }

    private fun getCurrentListFiles() {
        launchOnIO {
            Log.d(TAG, "getCurrentListFiles: Try to get")
            // TODO: Handle error situation
            val prefix = cache.coreFTPClient.getCurrentPath() ?: ""

            val list = cache.coreFTPClient.getList().map {
                it.toFileItemInfo(prefix)
            }
            Log.d(TAG, "getCurrentListFiles ${list.size}")

            setState {
                copy(
                    path = prefix,
                    fileList = list,
                    loadingState = LoadingState.SUCCESS
                )
            }
        }
    }

    private fun onPathChanged(targetPath: String) {
        launchOnIO {
            cache.coreFTPClient.changeAndGetFiles(targetPath,
                onBegin = { setState { copy(loadingState = LoadingState.LOADING) } },
                onSuccess = {
                    setState {
                        copy(
                            loadingState = LoadingState.SUCCESS,
                            fileList = it.map { it.toFileItemInfo(targetPath) },
                            path = targetPath
                        )
                    }
                    Log.d(TAG, "onPathChanged: Path successfully changed")
                },
                onFail = { setState { copy(loadingState = LoadingState.FAIL) } }
            )
        }
    }

    private fun refresh() {
        launchOnIO {
            setState { copy(loadingState = LoadingState.LOADING) }
            val files = cache.coreFTPClient.getFiles()
            val currentPath = cache.coreFTPClient.getCurrentPath()
            if (files != null) {
                setState {
                    copy(
                        loadingState = LoadingState.SUCCESS,
                        fileList = files.map { it.toFileItemInfo(prefix = currentPath) },
                    )
                }
            } else {
                setState { copy(loadingState = LoadingState.FAIL) }
            }
        }
    }

    private fun onFileSelected(fileName: String, select: Boolean, isDir: Boolean) {
        setState {
            val newSet = (if (isDir) selectDirList else selectedFileList).toMutableSet().apply {
                if (select) {
                    this.add(fileName)
                } else {
                    this.remove(fileName)
                }
            }
            copy(
                selectedFileList = if (isDir) selectedFileList else newSet,
                selectDirList = if (isDir) newSet else selectDirList
            )
        }
    }

    private fun downloadMultipleFiles() {

    }

    private fun createDirectory(dirName: String) {
        launchOnIO {
            setState { copy(createDirDialog = createDirDialog.copy(loadingStatus = LoadingState.LOADING)) }
            val res = cache.coreFTPClient.createDirectory(dirName)
            if (res) {
                setState { copy(createDirDialog = createDirDialog.copy(loadingStatus = LoadingState.SUCCESS)) }
                delay(1000)
                setState { copy(createDirDialog = CreateDirDialog()) }
                refresh()
            } else {
                setState { copy(createDirDialog = createDirDialog.copy(loadingStatus = LoadingState.FAIL)) }
            }
        }

    }

    private fun handleDismissDialog() {
        setState { copy(createDirDialog = CreateDirDialog()) }
    }

    @AssistedFactory
    interface FilePathViewModelFactory {
        fun create(host: String): FilePathViewModel
    }

    // Maybe it's better to be placed in Compose File
    fun bottomAppBarActionEvents(fileBottomAppBarAction: FileBottomAppBarAction) {
        when (fileBottomAppBarAction) {
            FileBottomAppBarAction.REFRESH -> {
                sendIntent(FilePathUiIntent.AppBar.Refresh)
            }

            FileBottomAppBarAction.DELETE -> {
                sendIntent(FilePathUiIntent.AppBar.OpenDeleteFileDialog(uiState.value.selectedFileList))
            }

            FileBottomAppBarAction.CREATE_FOLDER -> {
                sendIntent(FilePathUiIntent.AppBar.ClickCreateDirIcon)
            }

            else -> {}
        }
    }

    // Maybe it's better to be placed in Compose File
    fun bottomAppBarFABActionEvents(
        status: FileSelectStatus
    ) {
        when (status) {
            FileSelectStatus.Single -> {
                sendIntent(FilePathUiIntent.AppBar.OpenFileSelectWindow)
            }

            FileSelectStatus.Multiple -> {
//                sendIntent(FilePathUiIntent.AppBar.DownloadMultipleFiles)
            }
        }
    }

    private fun onSelectModeChanged(select: Boolean) {
        if (select.xor(uiState.value.appBarStatus == FileSelectStatus.Single)) {
            return
        }
        setState {
            copy(
                appBarStatus = if (select) FileSelectStatus.Multiple else FileSelectStatus.Single,
                selectedFileList = mutableSetOf(),
                selectDirList = mutableSetOf()
            )
        }
    }

}

