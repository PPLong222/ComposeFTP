package indi.pplong.composelearning.core.file.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
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
    @Assisted val host: String
) :
    BaseViewModel<FilePathUiState, FilePathUiIntent, FilePathUiEffect>() {
    private val TAG = javaClass.name
    private lateinit var cache: FTPClientCache
    private val thumbnailMutex = Mutex()

    // TODO: move to [ThumbnailClient]?
    private val thumbnailQueue: Queue<FileItemInfo> = LinkedList()

    private var thumbnailJob: Job? = null

    private val signal = Channel<Unit>(Channel.CONFLATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it["185.211.4.19"] }
            .flatMapLatest { data -> data.downloadQueue.map { it.size } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uploadQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it["185.211.4.19"] }
            .flatMapLatest { data -> data.uploadQueue.map { it.size } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val idleQueueSize =
        globalViewModel.pool.serverFTPMap.mapNotNull { it["185.211.4.19"] }
            .flatMapLatest { data -> data.idledClientsQueue.map { it.size } }

    val transferringCount = downloadQueueSize.combine(uploadQueueSize) { a, b -> a + b }


    init {
        Log.d(TAG, "Init ViewModfel: $host")

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
        }
    }

    private fun handleAppBarIntent(intent: FilePathUiIntent.AppBar) {
        when (intent) {
            is FilePathUiIntent.AppBar.Upload -> {
                uploadSingleFile(intent.transferringFile, intent.uri)
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
            val fileSize = uiState.value.fileList.filter { it.isSelected }
            val deleteFileRes =
                cache.coreFTPClient.deleteFile(uiState.value.fileList.filter { it.isSelected }
                    .filter { !it.isDir }
                    .map { it.name })
            val deleteDirRes =
                cache.coreFTPClient.deleteDirectory(uiState.value.fileList.filter { it.isSelected }
                    .filter { it.isDir }
                    .map { it.name })
            delay(2000)
            if (deleteFileRes && deleteDirRes) {
                setState {
                    copy(
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

    private fun uploadSingleFile(transferringFile: TransferringFile, uri: Uri) {
        val file = transferringFile.copy(
            transferredFileItem = transferringFile.transferredFileItem.copy(serverHost = cache.coreFTPClient.host)
        )

        launchOnIO {
            cache.uploadFile(file, uri)
        }
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
            copy(appBarStatus = FileSelectStatus.Single)
        }
        launchOnIO {
            var tempFileList = uiState.value.fileList.toMutableList()
            uiState.value.fileList.filter { it.isSelected }.forEach {
                val newFileInfo =
                    it.copy(transferStatus = TransferStatus.Loading)
                tempFileList[tempFileList.indexOf(it)] = newFileInfo
                launch {
                    cache.downloadFile(newFileInfo)
                }
            }
            setState {
                copy(fileList = tempFileList)
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

