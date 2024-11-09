package indi.pplong.composelearning.core.file.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R

/**
 * Description:
 * @author PPLong
 * @date 11/8/24 5:40 PM
 */

@Composable
@Preview
fun FileSortTypeMenu(
    sortMode: FileSortTypeMode = FileSortTypeMode(FileSortType.Name, true),
    onChange: (FileSortTypeMode) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_sort),
                contentDescription = "Localized description"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FileSortType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.stringRes)) },
                    onClick = {
                        if (type != sortMode.fileSortType) {
                            onChange(FileSortTypeMode(type, true))
                        } else {
                            onChange(sortMode.copy(isAscending = sortMode.isAscending.not()))
                        }
                    },
                    trailingIcon = {
                        if (type == sortMode.fileSortType) {
                            Icon(
                                painter = painterResource(if (sortMode.isAscending) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

enum class FileSortType(@StringRes val stringRes: Int) {
    Name(R.string.name),
    Date(R.string.date),
    Size(R.string.size),
    Type(R.string.type)
}

data class FileSortTypeMode(
    val fileSortType: FileSortType,
    val isAscending: Boolean
)