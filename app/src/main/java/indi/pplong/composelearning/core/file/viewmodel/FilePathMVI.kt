package indi.pplong.composelearning.core.file.viewmodel

import indi.pplong.composelearning.core.base.mvi.UiEffect
import indi.pplong.composelearning.core.base.mvi.UiIntent
import indi.pplong.composelearning.core.base.mvi.UiState
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.base.state.RequestingState
import indi.pplong.composelearning.core.file.model.ActiveServer
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.load.model.TransferringFile
import java.io.InputStream
import java.io.OutputStream

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:07 PM
 */
data class FilePathUiState(
    val path: String = "",
    val activeList: List<ActiveServer> = arrayListOf(),
    val fileList: List<FileItemInfo> = arrayListOf(),
    val loadingState: LoadingState = LoadingState.LOADING,
    val actionLoadingState: RequestingState = RequestingState.DONE
) : UiState

sealed class FilePathUiIntent : UiIntent {
    data class MoveForward(val path: String) : FilePathUiIntent()
    data class Download(
        val outputStream: OutputStream,
        val fileItemInfo: FileItemInfo,
        val localUri: String
    ) :
        FilePathUiIntent()

    data class Upload(val transferringFile: TransferringFile, val inputStream: InputStream) :
        FilePathUiIntent()

    /**
     * @param fileName: fileName of current path
     */
    data class DeleteFile(val fileName: String) : FilePathUiIntent()
    data object DismissDialog : FilePathUiIntent()
    data class OpenDeleteFileDialog(val fileName: String) : FilePathUiIntent()
}

sealed class FilePathUiEffect : UiEffect {
    data class ShowDeleteDialog(val fileName: String) : FilePathUiEffect()
    data object DismissDeleteDialog : FilePathUiEffect()
    data class ActionFailed(val text: String) : FilePathUiEffect()
}