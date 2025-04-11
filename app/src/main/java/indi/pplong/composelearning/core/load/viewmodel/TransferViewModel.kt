package indi.pplong.composelearning.core.load.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.cache.GlobalCacheList
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import indi.pplong.composelearning.core.file.model.TransferredFileItem
import indi.pplong.composelearning.core.util.FileUtil
import indi.pplong.composelearning.core.util.MD5Utils
import indi.pplong.composelearning.core.util.getContentUri
import indi.pplong.composelearning.ftp.clients.ThumbnailFTPClient.Companion.MAX_CACHE_FILE_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Description:
 * @author PPLong
 * @date 10/24/24 9:37 PM
 */
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val globalViewModel: GlobalRepository,
    private val fileDao: TransferredFileDao
) : BaseViewModel<TransferUiState, TransferUiIntent, TransferUiEffect>() {

    private val TAG = javaClass.name

    init {
        //
        launchOnIO {
            globalViewModel.pool.downloadFTPSet.collect {
                it.forEach { client ->
                    launch {
                        client.transferFileFlow.collect { file ->
                            setState {
                                copy(downloadFileList = downloadFileList.toMutableList().apply {
                                    val index = indexOfFirst {
                                        it.pathPrefix + "/" + it.name == file.pathPrefix + "/" + file.name
                                    }
                                    if (index in downloadFileList.indices) {
                                        if (file.transferStatus is TransferStatus.Transferring) {
                                            set(index, file)
                                        } else if (file.transferStatus is TransferStatus.Successful) {
                                            removeAt(index)
                                        }
                                    } else if (file.transferStatus is TransferStatus.Transferring) {
                                        add(file)
                                    }
                                })
                            }
                            if (file.transferStatus == TransferStatus.Successful) {
                                getDownloadedFileList()
                            }
                        }
                    }

                }
            }
        }
        launchOnIO {
            globalViewModel.pool.uploadFTPSet.collect {
                it.forEach { client ->
                    launch {
                        client.uploadFileFlow.collect { file ->
                            setState {
                                copy(uploadFileList = uploadFileList.toMutableList().apply {
                                    val index = indexOfFirst {
                                        it.transferredFileItem.remoteName == file.transferredFileItem.remoteName && it.transferredFileItem.remotePathPrefix == file.transferredFileItem.remotePathPrefix
                                    }
                                    if (index in uploadFileList.indices) {
                                        if (file.transferStatus is TransferStatus.Transferring) {
                                            set(index, file)
                                        } else if (file.transferStatus is TransferStatus.Successful) {
                                            removeAt(index)
                                        }
                                    } else if (file.transferStatus is TransferStatus.Transferring) {
                                        add(file)
                                    }
                                })
                            }
                            if (file.transferStatus == TransferStatus.Successful) {
                                getUploadedFileList()
                            }
                        }
                    }

                }
            }
        }

        getDownloadedFileList()
        getUploadedFileList()

    }

    private fun getUploadedFileList() {
        println("update")
        launchOnIO {
            val list = fileDao.getUploadedItems().sortedByDescending { it.timeMills }
            setState {
                copy(
                    alreadyUploadFileList = list
                )
            }
        }

    }

    private fun getDownloadedFileList() {
        Log.d(TAG, "getDownloadedFileList: Download")
        launchOnIO {
            val list = fileDao.getDownloadedItems().sortedByDescending { it.timeMills }
            setState {
                copy(
                    alreadyDownloadFileList = list
                )
            }
        }
    }


    override fun initialState(): TransferUiState {
        return TransferUiState()
    }

    override suspend fun handleIntent(intent: TransferUiIntent) {
        when (intent) {
            is TransferUiIntent.SwitchTab -> {
                setState { copy(curIndex = intent.index) }
            }

            is TransferUiIntent.MoveForward -> {}
            is TransferUiIntent.CacheImage -> saveFileToLocal(
                intent.context,
                intent.transferredItemInfo
            )
        }
    }

    fun saveFileToLocal(context: Context, transferredFileItem: TransferredFileItem) {
        launchOnIO {
            val key =
                MD5Utils.digestMD5AsString((transferredFileItem.serverHost + transferredFileItem.remotePathPrefix + transferredFileItem.remoteName + transferredFileItem.timeMills).toByteArray())
            val file = File(context.cacheDir, key)
            context.contentResolver.openInputStream(transferredFileItem.localUri.toUri())
                ?.use { inputStream ->
                    BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        var totalBytesRead = 0
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (totalBytesRead + bytesRead > MAX_CACHE_FILE_SIZE) {
                                outputStream.write(buffer, 0, MAX_CACHE_FILE_SIZE - totalBytesRead)
                                break
                            }
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                        }
                    }
                }


            val bitmap = FileUtil.getVideoThumbnailWithRetriever(
                context,
                file.getContentUri(context)!!,
                48,
                48
            )

            val finalFile =
                MD5Utils.bitmapToCompressedFile(context, bitmap!!, key)
            val uri = finalFile.getContentUri(context)

            GlobalCacheList.map.put(
                finalFile.name.removeSuffix(".jpg"),
                finalFile.getContentUri(context).toString()
            )

            setState {
                val transferredFileItems = alreadyUploadFileList.map {
                    if (it == transferredFileItem) {
                        val copy = it.copy(localImageUri = uri.toString())
                        launch(Dispatchers.IO) {
                            fileDao.update(copy)
                        }
                        copy
                    } else {
                        it
                    }
                }
                copy(alreadyUploadFileList = transferredFileItems)
            }

            launch(Dispatchers.IO) {
                file.delete()
                Log.d(TAG, "launchThumbnailWork: Delete temp pre file: ${file.name}")
            }
        }


    }
}