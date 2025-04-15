package indi.pplong.composelearning.core.host.ui

import android.content.Intent
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.state.ConfigureState
import indi.pplong.composelearning.core.base.state.EditState
import indi.pplong.composelearning.core.base.ui.PasswordTextField
import indi.pplong.composelearning.core.host.model.ConnectivityTestState
import indi.pplong.composelearning.core.host.viewmodel.EditServerIntent
import indi.pplong.composelearning.core.host.viewmodel.EditServerUiState
import indi.pplong.composelearning.core.util.ServerPortInfo
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme
import kotlinx.coroutines.launch

/**
 * Description:
 * @author PPLong
 * @date 9/28/24 10:10 PM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerBottomSheet(
    onDismissRequest: () -> Unit = {},
    uiState: EditServerUiState = EditServerUiState(),
    onIntent: (EditServerIntent) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LaunchedEffect(uiState.editState) {
            if (uiState.editState == EditState.SUCCESS) {
                onDismissRequest()
            }
        }

        AnimatedContent(uiState.configureState, label = "") { state ->
            when (state) {
                ConfigureState.CONNECTING -> EditServerBottomSheetContent(
                    uiState,
                    onIntent
                )

                ConfigureState.CONFIGURING -> ConfigureHostInfo(
                    uiState,
                    onIntent
                )
            }
        }

    }
}

@Composable
internal fun EditServerBottomSheetContent(
    uiState: EditServerUiState = EditServerUiState(),
    onIntent: (EditServerIntent) -> Unit = {}
) {
    val animatedProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
//        Box(
//            modifier = Modifier
//                .matchParentSize()
//                .background(Color(0xFFFFFFFF))
//        )
//        Box(
//            modifier = Modifier
//                .matchParentSize()
//                .graphicsLayer {
//                    scaleX = animatedProgress.value
//                    scaleY = animatedProgress.value
//                    transformOrigin = TransformOrigin(0f, 1f) // 左下角放大
//                }
//                .background(Color(0xFF4CAF50), shape = RoundedCornerShape(topEnd = 32.dp))
//                .zIndex(1f)
//        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .zIndex(2F)
        ) {

            Text(
                style = MaterialTheme.typography.titleLarge,
                text = stringResource(R.string.add_a_host),
                modifier = Modifier.padding()
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Using SFTP")
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.host.isSFTP,
                    onCheckedChange = { isSFTP ->
                        scope.launch {
                            animatedProgress.animateTo(
                                targetValue = if (isSFTP) 1F else 0F,
                                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                            )
                        }

                        onIntent(
                            EditServerIntent.OnChangeHostInfo(
                                uiState.host.copy(
                                    isSFTP = isSFTP
                                )
                            )
                        )
                    },
                )
            }


            OutlinedTextField(
                value = uiState.host.host,
                onValueChange = { onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(host = it))) },
                label = { Text(stringResource(R.string.host)) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                text = uiState.host.password,
                onValueChange = {
                    onIntent(
                        EditServerIntent.OnChangeHostInfo(
                            uiState.host.copy(
                                password = it
                            )
                        )
                    )
                },
                labelString = "Password",
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedTextField(
                    value = uiState.host.user,
                    onValueChange = {
                        onIntent(
                            EditServerIntent.OnChangeHostInfo(
                                uiState.host.copy(
                                    user = it
                                )
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.user)) },
                    modifier = Modifier
                        .weight(0.7f)
                )
                OutlinedTextField(
                    value = uiState.host.port.toString(),
                    onValueChange = { portStr ->
                        val newPort = portStr.toIntOrNull()
                            ?.coerceIn(ServerPortInfo.MIN_PORT, ServerPortInfo.MAX_PORT)
                            ?: 0
                        onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(port = newPort)))
                    },
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(0.3f),
                    supportingText = {
                        Text("${ServerPortInfo.MIN_PORT}-${ServerPortInfo.MAX_PORT}")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = uiState.state,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(1000)
                    ) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "",
            ) { targetState ->
                when (targetState) {
                    ConnectivityTestState.FAIL -> {
                        // TODO: tips of multiple kinds
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.network_error_tip),
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { onIntent(EditServerIntent.TestConnectivity) },
                            ) {
                                Text(stringResource(R.string.test_again))
                            }
                        }

                    }

                    ConnectivityTestState.INITIAL ->
                        Button(
                            onClick = { onIntent(EditServerIntent.TestConnectivity) },
                        ) {
                            Text(stringResource(R.string.test_connectivity))
                        }


                    ConnectivityTestState.TESTING ->
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(32.dp)
                                .padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                    ConnectivityTestState.SUCCESS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row {
                                Icon(
                                    Icons.Filled.ThumbUp,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.congratulations),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { onIntent(EditServerIntent.NextToConfigure) },
                            ) {
                                Text(stringResource(R.string.next))
                            }
                        }
                    }
                }
            }


        }
    }
}


@Composable
fun ConfigureHostInfo(
    uiState: EditServerUiState = EditServerUiState(),
    onIntent: (EditServerIntent) -> Unit = {},
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onIntent(
                EditServerIntent.OnChangeHostInfo(
                    uiState.host.copy(
                        downloadDir = uri.toString()
                    )
                )
            )
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
    var defaultDownloadDir by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        defaultDownloadDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = "Customize",
        )

        Text(
            style = MaterialTheme.typography.titleMedium,
            text = "Pick an Icon",
        )


        OutlinedTextField(
            value = uiState.host.nickname,
            onValueChange = { onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(nickname = it))) },
            label = { Text(stringResource(R.string.nickname)) },
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Customize download path")
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                launcher.launch(null)
            }, shape = RoundedCornerShape(size = 8.dp)) {
                Icon(painter = painterResource(R.drawable.ic_file_open), contentDescription = null)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Path: ${uiState.host.downloadDir ?: defaultDownloadDir}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }


        Button(
            onClick = { onIntent(EditServerIntent.SaveHost) },
            modifier = Modifier.padding(12.dp)
        ) {
            Text("Save")
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ConfigureHostInfoPreview() {
    ComposeLearningTheme {
        ConfigureHostInfo()
    }
}

@Composable
@Preview(showBackground = true)
fun EditServerBottomSheetContentPreview() {
    ComposeLearningTheme {
        EditServerBottomSheetContent()
    }
}