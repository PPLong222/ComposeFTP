package indi.pplong.composelearning.core.file.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.CommonFileInfo
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.model.getKey
import indi.pplong.composelearning.core.file.model.toFileItemInfo
import indi.pplong.composelearning.core.file.ui.FileBottomAppBarAction
import indi.pplong.composelearning.core.file.ui.FileSortType
import indi.pplong.composelearning.core.file.ui.FileSortTypeMode
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.ftp.FTPClientCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    @ApplicationContext val context: Context,
    @Assisted val hostKey: Long
) :
    BaseViewModel<FilePathUiState, FilePathUiIntent, FilePathUiEffect>() {
    private val TAG = javaClass.name
    private lateinit var cache: FTPClientCache
    private val thumbnailMutex = Mutex()

    // TODO: move to [ThumbnailClient]?
    private val thumbnailQueue: Queue<FileItemInfo> = LinkedList()

    private var thumbnailJob: Job? = null

    private val signal = Channel<Unit>(Channel.CONFLATED)
    private val host = globalViewModel.pool.serverFTPMap.value[hostKey]?.config?.host ?: ""

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadFileList = globalViewModel.pool.serverFTPMap.map { it[hostKey] }.filterNotNull()
        .flatMapLatest { cache ->
            cache.downloadQueue.flatMapLatest { set ->
                if (set.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = set.map { it.transferFlow() }
                    combine(flows) { arr -> arr.toList() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it[hostKey] }
            .flatMapLatest { data -> data.downloadQueue.map { it.size } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uploadQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it[hostKey] }
            .flatMapLatest { data -> data.uploadQueue.map { it.size } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val idleQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it[hostKey] }
            .flatMapLatest { data -> data.idledClientsQueue.map { it.size } }

    val transferringCount = downloadQueueSize.combine(uploadQueueSize) { a, b -> a + b }


    init {
        Log.d(TAG, "Init ViewModfel: $hostKey")
        globalViewModel.pool.serverFTPMap.value[hostKey]?.let {
            cache = it
            launchOnIO {
                val currentPath = it.getCurrentPath() ?: "/"

                onPathChanged(currentPath)
            }
        } ?: run {
            throw Exception()
        }
        launchOnIO {
            transferringCount.drop(1).collect {
                setState {
                    copy(isTransferStatusViewed = false)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val task = thumbnailMutex.withLock {
                    if (thumbnailQueue.isEmpty()) {
                        null
                    } else {
                        thumbnailQueue.poll()
                    }
                }
                if (task != null) {
                    thumbnailJob = launch {
                        try {
                            val uri = cache.launchThumbnailJob(task.name, task.md5)
                            setState {
                                copy(fileList = fileList.map {
                                    if (it.md5 == task.md5) {
                                        it.copy(localImageUri = uri.toString())
                                    } else {
                                        it
                                    }
                                })
                            }
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Thumbnail JOB Cancel: ")
                        }
                    }
                    thumbnailJob?.join()
                } else {
                    signal.receive()
                }
            }
        }

        viewModelScope.launch {
            downloadFileList.collect { downloadList ->
                val fileList = uiState.value.fileList
                val map = downloadList.associate {
                    it.transferredFileItem.getKey(hostKey) to it
                }
                setState {
                    copy(
                        fileList = fileList.map {
                            val updatedFile = map[it.getKey(hostKey)]
                            if (updatedFile != null) {
                                it.copy(transferStatus = updatedFile.transferStatus)
                            } else {
                                it
                            }
                        }
                    )
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

            is FilePathUiIntent.Browser.OnFileSelect -> onFileSelected(
                intent.fileInfo
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

            is FilePathUiIntent.Browser.DeleteFile -> {
                deleteFile(intent.fileInfo)
            }
        }
    }

    private fun handleAppBarIntent(intent: FilePathUiIntent.AppBar) {
        when (intent) {
            is FilePathUiIntent.AppBar.Upload -> {
                uploadSingleFile(
                    intent.localUri,
                    intent.remotePath
                )
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

            is FilePathUiIntent.AppBar.SetTransferSheetShow -> {

                setState {
                    copy(showTransferSheet = intent.isShow, isTransferStatusViewed = true)
                }
            }
        }
    }

    private fun handleDialogIntent(intent: FilePathUiIntent.Dialog) {
        when (intent) {
            FilePathUiIntent.Dialog.DeleteFile -> deleteFile()
            FilePathUiIntent.Dialog.DismissAllDialog -> {
                // TODO: Unify to UiState
                sendEffect {
                    FilePathUiEffect.DismissDeleteDialog
                }
                handleDismissDialog()
            }

            is FilePathUiIntent.Dialog.CreateDirectory -> {
                createDirectory(intent.fileName)
            }

            is FilePathUiIntent.Dialog.RenameFile -> {
                renameFile(intent.originalFileName, intent.updatedName)
            }

            is FilePathUiIntent.Dialog.OpenRenameDialog -> {
                sendEffect {
                    FilePathUiEffect.ShowFileRenameDialog(intent.originalFileName)
                }
            }
        }
    }

    private fun handleSnackBarIntent(intent: FilePathUiIntent.SnackBarHost) {
        when (intent) {
            FilePathUiIntent.SnackBarHost.PerformClick -> {}
        }
    }

    private fun deleteFile(fileInfo: FileItemInfo? = null) {
        launchOnIO {
            var handleList =
                if (fileInfo == null) uiState.value.fileList else listOf(fileInfo.copy(isSelected = true))
            val fileSize = handleList.filter { it.isSelected }.size
            val deleteFileRes =
                cache.coreFTPClient.deleteFile(handleList.filter { it.isSelected }
                    .filter { !it.isDir }
                    .map { it.name })
            val deleteDirRes =
                cache.coreFTPClient.deleteDirectory(handleList.filter { it.isSelected }
                    .filter { it.isDir }
                    .map { it.name })
            if (deleteFileRes && deleteDirRes) {
                setState {
                    copy(
                        appBarStatus = FileSelectStatus.Single
                    )
                }
                sendEffect { FilePathUiEffect.OnDeleteFile }
                sendEffect {
                    FilePathUiEffect.ShowSnackBar(
                        message = "You delete $fileSize file",
                        actionLabel = "Undo",
                        withDismissAction = false,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

    }

    private fun uploadSingleFile(uri: Uri, remotePath: String) {
        val fileSize = FileUtil.getFileSize(context, uri)
        val fileName = FileUtil.getFileName(context, uri)

        val file = CommonFileInfo(
            name = fileName,
            isDir = false,
            path = remotePath,
            size = fileSize,
            localUri = uri.toString(),
        )

        sendEffect { FilePathUiEffect.LaunchTransferService(uploadFileList = listOf(file)) }
    }

    private fun onPathChanged(targetPath: String) {
        launchOnIO {
            thumbnailQueue.clear()
            thumbnailJob?.cancel()

            setState { copy(loadingState = LoadingState.LOADING) }

            val fileList = runCatching { cache.changePathAndGetFiles(targetPath) }
            fileList.fold(
                onSuccess = {
                    setState {
                        val convertedList = it.map {

                            val key =
                                it.getKey(hostKey)
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

        launchOnIO {
            thumbnailMutex.withLock {
                thumbnailQueue.offer(fileItemInfo)
            }
            signal.send(Unit)
        }

//        thumbnailJob = viewModelScope.launch(Dispatchers.IO) {
//            val uri = thumbnailMutex.withLock {
//                thumbnailQueue.peek()
//                val launchThumbnailJob =
//                    cache.launchThumbnailJob(fileItemInfo.name, fileItemInfo.md5)
//                launchThumbnailJob
//            }
//
//            thumbnailQueue.poll()
//            setState {
//                copy(fileList = fileList.map {
//                    if (it.md5 == fileItemInfo.md5) {
//                        it.copy(localImageUri = uri.toString())
//                    } else {
//                        it
//                    }
//                })
//            }
//        }

    }

    private fun refresh() {
        launchOnIO {
            setState { copy(loadingState = LoadingState.LOADING) }
            val files = cache.coreFTPClient.list()
            val currentPath = cache.coreFTPClient.getCurrentPath()
            if (files != null) {
                setState {
                    copy(
                        loadingState = LoadingState.SUCCESS,
                        fileList = files.map {
                            val key = it.getKey(hostKey)
                            it.toFileItemInfo(
                                prefix = currentPath ?: "/",
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

    private fun onFileSelected(fileInfo: FileItemInfo) {
        setState {
            val newFileList = uiState.value.fileList.toMutableList().map {
                if (it.md5 == fileInfo.md5) {
                    it.copy(isSelected = it.isSelected.not())
                } else {
                    it
                }
            }
            copy(fileList = newFileList)
        }
    }

    private fun downloadMultipleFiles() {
        setState {
            copy(
                fileList = fileList.map {
                    if (it.isSelected) {
                        it.copy(transferStatus = TransferStatus.Loading)
                    } else {
                        it
                    }
                },
                appBarStatus = FileSelectStatus.Single
            )
        }
        val selectedFiles = uiState.value.fileList.filter { it.isSelected }.map {
            CommonFileInfo(
                name = it.name,
                path = it.pathPrefix,
                isDir = it.isDir,
                mtime = it.timeStamp,
                localImageUri = it.localImageUri,
                size = it.size,
                user = it.user
            )
        }
        sendEffect { FilePathUiEffect.LaunchTransferService(downloadFileList = selectedFiles) }

//        selectedFiles.forEach { file ->
//            viewModelScope.launch(Dispatchers.IO) {
//                cache.downloadFile(file)
//            }
//        }
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

    private fun renameFile(originalName: String, newName: String) {
        launchOnIO {
            val res = cache.coreFTPClient.renameFile(originalName, newName)
            if (res) {
                refresh()
            }
        }
    }

    private fun moveFile(originPath: String, targetPath: String) {

//        launchOnIO {
//            val res = runCatching { cache.coreFTPClient.moveFile(originPath, targetPath) }
//            res.fold(
//                onSuccess = {
//
//                },
//                onFailure = {
//
//                }
//            )
//        }

    }

    @AssistedFactory
    interface FilePathViewModelFactory {
        fun create(hostKey: Long): FilePathViewModel
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
                fileList = fileList.map {
                    // fix bug when selecting not effect
                    if (it.transferStatus == TransferStatus.Successful || it.transferStatus == TransferStatus.Failed) {
                        it.copy(transferStatus = TransferStatus.Initial, isSelected = false)
                    } else {
                        it.copy(isSelected = false)
                    }
                }
            )
        }
    }

}

