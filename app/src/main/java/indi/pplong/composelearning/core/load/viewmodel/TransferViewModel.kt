package indi.pplong.composelearning.core.load.viewmodel

import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.pplong.composelearning.core.base.GlobalRepository
import indi.pplong.composelearning.core.base.mvi.BaseViewModel
import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileDao
import kotlinx.coroutines.launch
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
        }
    }
}