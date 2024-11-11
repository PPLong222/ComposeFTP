package indi.pplong.composelearning.core.file.viewmodel

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.toMutableStateList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.model.toFileItemInfo
import indi.pplong.composelearning.core.file.ui.FileBottomAppBarAction
import indi.pplong.composelearning.core.file.ui.FileSortType
import indi.pplong.composelearning.core.file.ui.FileSortTypeMode
import indi.pplong.composelearning.core.load.model.TransferringFile
import indi.pplong.composelearning.core.util.MD5Utils
import indi.pplong.composelearning.ftp.FTPClientCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.util.LinkedList
import java.util.Queue

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:16 PM
 */
@HiltViewModel(assistedFactory = FilePathViewModel.FilePathViewModelFactory::class)
class FilePathViewModel @AssistedInject constructor(
    private val globalViewModel: GlobalRepository,
    @Assisted val host: String
) :
    BaseViewModel<FilePathUiState, FilePathUiIntent, FilePathUiEffect>() {
    private val TAG = javaClass.name
    private lateinit var cache: FTPClientCache
    private val thumbnailMutex = Mutex()

    // TODO: move to [ThumbnailClient]?
    private val thumbnailQueue: Queue<FileItemInfo> = LinkedList()

    init {
        Log.d(TAG, "Init ViewModfel: $host")
//        launchOnIO {
//            cache = globalViewModel.pool.getCacheByHost(host)!!
//            getCurrentListFiles()
//        }
        launchOnIO {
            globalViewModel.pool.serverFTPMap.collect {
                launch {
                    it.values.firstOrNull { it.coreFTPClient.host == host }?.let {
                        cache = it
                        onPathChanged(it.getCurrentPath())
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
                downloadSingleFile(intent.fileItemInfo, intent.localUri)
            }

            is FilePathUiIntent.Browser.OnFileSelect -> onFileSelected(
                intent.fileInfo,
                intent.select
            )

            is FilePathUiIntent.Browser.CacheItem -> {
                launchThumbnailJob(intent.fileItemInfo)
            }

            is FilePathUiIntent.Browser.OnFileSortModeChange -> {
                onFileTypeModeChanged(intent.fileSortMode)
            }

            is FilePathUiIntent.Browser.MoveFile -> {
                moveFile(intent.originPath, intent.targetPath)
            }
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
                    FilePathUiEffect.ShowDeleteDialog
                }
            }

            FilePathUiIntent.AppBar.OpenFileSelectWindow -> {
                sendEffect { FilePathUiEffect.ShowFileSelectWindow }
            }

            is FilePathUiIntent.AppBar.OnDownloadButtonClick -> {
                downloadMultipleFiles()
            }


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
                cache.coreFTPClient.deleteFile(uiState.value.selectedFileList.filter { !it.isDir }
                    .map { it.name })
            val deleteDirRes =
                cache.coreFTPClient.deleteDirectory(uiState.value.selectedFileList.filter { it.isDir }
                    .map { it.name })
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
        fileInfo: FileItemInfo,
        localUri: String
    ) {
        val newFileInfo =
            fileInfo.copy(transferStatus = TransferStatus.Loading)
        setState {
            copy(fileList = fileList.toMutableStateList().apply {
                set(indexOf(fileInfo), newFileInfo)
            })
        }
        launchOnIO {
            cache.downloadFile(newFileInfo, localUri)
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

    private fun onPathChanged(targetPath: String) {
        thumbnailQueue.clear()
        launchOnIO {
            setState { copy(loadingState = LoadingState.LOADING) }

            val fileList = runCatching { cache.changePathAndGetFiles(targetPath) }
            fileList.fold(
                onSuccess = {
                    setState {
                        val convertedList = it.map {
                            val key =
                                MD5Utils.digestMD5AsString((host + targetPath + it.name + it.timestamp.time.time).toByteArray())
                            it.toFileItemInfo(
                                targetPath,
                                key,
                                GlobalCacheList.map[key] ?: ""
                            )
                        }

                        copy(
                            loadingState = LoadingState.SUCCESS,
                            fileList = sortListBySortMode(convertedList),
                            path = targetPath
                        )
                    }
                },
                onFailure = { setState { copy(loadingState = LoadingState.FAIL) } }
            )
        }
    }

    private fun launchThumbnailJob(fileItemInfo: FileItemInfo) {
        if (thumbnailQueue.contains(fileItemInfo)) {
            return
        }
        thumbnailQueue.offer(fileItemInfo)
        launchOnIO {
            val withLock = thumbnailMutex.withLock {
                thumbnailQueue.peek()
                val launchThumbnailJob =
                    cache.launchThumbnailJob(fileItemInfo.name, fileItemInfo.md5)
                launchThumbnailJob
            }
            thumbnailQueue.poll()
            setState {
                copy(fileList = fileList.map {
                    if (it.md5 == fileItemInfo.md5) {
                        it.copy(localUri = withLock.toString())
                    } else {
                        it
                    }
                })
            }
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
                        fileList = files.map {
                            val key =
                                MD5Utils.digestMD5AsString((host + currentPath + it.name + it.timestamp.time.time).toByteArray())
                            it.toFileItemInfo(
                                prefix = currentPath,
                                key,
                                GlobalCacheList.map[key] ?: ""
                            )
                        }
                    )
                }
            } else {
                setState { copy(loadingState = LoadingState.FAIL) }
            }
        }
    }

    private fun onFileSelected(fileInfo: FileItemInfo, select: Boolean) {
        setState {
            val newSet = selectedFileList.toMutableSet().apply {
                if (select) add(fileInfo) else remove(fileInfo)
            }
            copy(selectedFileList = newSet)
        }
    }

    private fun downloadMultipleFiles() {
        setState {
            copy(appBarStatus = FileSelectStatus.Single)
        }
        launchOnIO {
            uiState.value.selectedFileList.forEach {
                val newFileInfo =
                    it.copy(transferStatus = TransferStatus.Loading)
                setState {
                    copy(fileList = fileList.toMutableStateList().apply {
                        set(indexOf(it), newFileInfo)
                    })
                }
                launchOnIO {
                    cache.downloadFile(newFileInfo)
                }
            }
        }
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

    private fun onFileTypeModeChanged(fileSortMode: FileSortTypeMode) {
        // Sort List by sort type
        val finalList = sortListBySortMode(fileSortMode = fileSortMode)
        setState { copy(fileSortMode = fileSortMode, fileList = finalList) }
    }

    private fun sortListBySortMode(
        list: List<FileItemInfo> = uiState.value.fileList,
        fileSortMode: FileSortTypeMode = uiState.value.fileSortMode
    ): List<FileItemInfo> {
        val sortedList =
            when (fileSortMode.fileSortType) {
                FileSortType.Name -> list.sortedBy { it.name.lowercase() }
                FileSortType.Date -> list.sortedBy { it.timeStamp }
                FileSortType.Size -> list.sortedBy { it.size }
                FileSortType.Type -> list.sortedBy { it.name }
            }
        val finalList = if (fileSortMode.isAscending) sortedList else sortedList.reversed()
        return finalList
    }

    private fun moveFile(originPath: String, targetPath: String) {

        launchOnIO {
            val res = runCatching { cache.coreFTPClient.moveFile(originPath, targetPath) }
            res.fold(
                onSuccess = {

                },
                onFailure = {

                }
            )
        }

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
                sendIntent(FilePathUiIntent.AppBar.OpenDeleteFileDialog)
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
                sendIntent(FilePathUiIntent.AppBar.OnDownloadButtonClick)
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
            )
        }
    }

}

