package indi.pplong.composelearning.core.file.viewmodel

import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import indi.pplong.composelearning.core.base.mvi.UiEffect
import indi.pplong.composelearning.core.base.mvi.UiIntent
import indi.pplong.composelearning.core.base.mvi.UiState
import indi.pplong.composelearning.core.base.state.LoadingState
import indi.pplong.composelearning.core.base.state.RequestingState
import indi.pplong.composelearning.core.file.model.FileItemInfo
import indi.pplong.composelearning.core.file.model.FileSelectStatus
import indi.pplong.composelearning.core.file.ui.FileSortType
import indi.pplong.composelearning.core.file.ui.FileSortTypeMode
import indi.pplong.composelearning.core.load.model.TransferringFile

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
//    val selectedFileList: Set<FileItemInfo> = mutableSetOf(),
    val createDirDialog: CreateDirDialog = CreateDirDialog(),
    val fileSortMode: FileSortTypeMode = FileSortTypeMode(fileSortType = FileSortType.Name, true),
    val showTransferSheet: Boolean = false,
    val isTransferStatusViewed: Boolean = true
) : UiState

sealed class FilePathUiIntent : UiIntent {
    sealed class Browser : FilePathUiIntent() {
        data class MoveForward(val path: String) : Browser()

        data class OnFileSelect(val fileInfo: FileItemInfo) :
            Browser()

        data class CacheItem(val fileItemInfo: FileItemInfo) : Browser()

        data class OnFileSortModeChange(val fileSortMode: FileSortTypeMode) : Browser()

        data class MoveFile(val originPath: String, val targetPath: String) : Browser()

        data class DeleteFile(val fileInfo: FileItemInfo) : Browser()
    }

    sealed class AppBar : FilePathUiIntent() {
        data object OpenDeleteFileDialog : AppBar()
        data object OpenFileSelectWindow : AppBar()
        data object Refresh : AppBar()
        data class SelectFileMode(val select: Boolean) : AppBar()
        data object OnDownloadButtonClick : AppBar()

        data class Upload(val transferringFile: TransferringFile, val uri: Uri) :
            AppBar()

        data object ClickCreateDirIcon : AppBar()

        data class SetTransferSheetShow(val isShow: Boolean) : AppBar()

    }

    sealed class Dialog : FilePathUiIntent() {
        // Delete File Dialog
        data object DismissAllDialog : Dialog()

        /**
         * @param fileName: fileName of current path
         */
        data object DeleteFile : Dialog()

        // Create Directory Dialog
        data class CreateDirectory(val fileName: String) : Dialog()

        data class RenameFile(val originalFileName: String, val updatedName: String) : Dialog()

        data class OpenRenameDialog(val originalFileName: String) : Dialog()
    }

    sealed class SnackBarHost : FilePathUiIntent() {
        data object PerformClick : SnackBarHost()
    }
}

sealed class FilePathUiEffect : UiEffect {
    data object ShowDeleteDialog : FilePathUiEffect()
    data class ShowFileRenameDialog(val name: String) : FilePathUiEffect()
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