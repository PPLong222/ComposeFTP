package indi.pplong.composelearning.core.file.viewmodel

import androidx.compose.material3.SnackbarDuration
import indi.pplong.composelearning.core.base.mvi.UiEffect
import indi.pplong.composelearning.core.base.mvi.UiIntent
import indi.pplong.composelearning.core.base.mvi.UiState
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.base.state.RequestingState
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
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
    val fileList: List<FileItemInfo> = arrayListOf(),
    val loadingState: LoadingState = LoadingState.LOADING,
    val actionLoadingState: RequestingState = RequestingState.DONE,
    val appBarStatus: FileSelectStatus = FileSelectStatus.Single,
    val selectedFileList: Set<String> = mutableSetOf(),
    val selectDirList: Set<String> = mutableSetOf(),
    val createDirDialog: CreateDirDialog = CreateDirDialog()
) : UiState

sealed class FilePathUiIntent : UiIntent {
    sealed class Browser : FilePathUiIntent() {
        data class MoveForward(val path: String) : Browser()
        data class Download(
            val outputStream: OutputStream,
            val fileItemInfo: FileItemInfo,
            val localUri: String
        ) :
            Browser()

        data class OnFileSelect(val fileName: String, val select: Boolean, val isDir: Boolean) :
            Browser()
    }

    sealed class AppBar : FilePathUiIntent() {
        data class OpenDeleteFileDialog(val fileName: Set<String>) : AppBar()
        data object OpenFileSelectWindow : AppBar()
        data object Refresh : AppBar()
        data class SelectFileMode(val select: Boolean) : AppBar()
        data class DownloadMultipleFiles(
            val outputStreamList: List<OutputStream>,
            val localUriList: List<String>
        ) :
            AppBar()

        data class Upload(val transferringFile: TransferringFile, val inputStream: InputStream) :
            AppBar()

        data object ClickCreateDirIcon : AppBar()

    }

    sealed class Dialog : FilePathUiIntent() {
        // Delete File Dialog
        data object DismissDialog : Dialog()

        /**
         * @param fileName: fileName of current path
         */
        data object DeleteFile : Dialog()

        // Create Directory Dialog
        data class CreateDirectory(val fileName: String) : Dialog()
    }

    sealed class SnackBarHost : FilePathUiIntent() {
        data object PerformClick : SnackBarHost()
    }
}

sealed class FilePathUiEffect : UiEffect {
    data class ShowDeleteDialog(val fileName: Set<String>) : FilePathUiEffect()
    data object DismissDeleteDialog : FilePathUiEffect()
    data object OnDeleteFile : FilePathUiEffect()
    data object ShowFileSelectWindow : FilePathUiEffect()
    data class ShowSnackBar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
    ) : FilePathUiEffect()
}

data class CreateDirDialog(
    val isShow: Boolean = false,
    val fileName: String = "",
    val loadingStatus: LoadingState = LoadingState.INITIAL
)