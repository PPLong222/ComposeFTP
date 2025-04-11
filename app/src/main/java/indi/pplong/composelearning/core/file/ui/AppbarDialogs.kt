package indi.pplong.composelearning.core.file.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.state.LoadingState

/**
 * Description:
 * @author PPLong
 * @date 11/1/24 10:26 PM
 */

@Composable
@Preview
fun DeleteFileConfirmDialog(
    modifier: Modifier = Modifier,
    onConfirmed: () -> Unit = {},
    onCancel: () -> Unit = {},

    ) {
    var isDeleting by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {
            println("123")
        },
        confirmButton = {
            if (!isDeleting) {
                Button(onClick = {
                    isDeleting = true
                    onConfirmed()
                }) {
                    Text("DELETE")
                }
            }

        },
        dismissButton = {
            if (!isDeleting) {
                Button(onClick = onCancel) {
                    Text("Back")
                }
            }
        },
        icon = {
            Icon(Icons.Default.Info, contentDescription = null)
        },
        title = {
            Text("Delete Confirmation")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Are you sure you are going to delete File?")
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp)
                    )
                }
            }


        }
    )
}


@Composable
@Preview
fun CreateDirDialog(
    loadingState: LoadingState = LoadingState.INITIAL,
    onConfirmed: (String) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var fileName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = {
                onConfirmed(fileName)
            }) {
                Text(stringResource(R.string.create))
            }

        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null
            )
        },
        title = {
            Text("Create A Directory")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please input the updated name:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
            }


        }
    )
}

@Composable
@Preview
fun RenameFileDialog(
    onConfirmed: (String, String) -> Unit = { _, _ -> },
    onCancel: () -> Unit = {},
    originalName: String = "Original File"
) {
    var updatedName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = {
                onConfirmed(originalName, updatedName)
            }) {
                Text(stringResource(R.string.create))
            }

        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_create_new_folder),
                contentDescription = null
            )
        },
        title = {

            Text("Rename a file")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please input the directory name:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    supportingText = {
                        if (updatedName == originalName) {
                            Text(
                                "Updated name can't be the same with original value.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }


        }
    )
}

