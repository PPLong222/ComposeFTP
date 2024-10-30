package indi.pplong.composelearning.core.host.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.base.state.ConfigureState
import indi.pplong.composelearning.core.base.state.EditState
import indi.pplong.composelearning.core.base.ui.PasswordTextField
import indi.pplong.composelearning.core.host.model.ConnectivityTestState
import indi.pplong.composelearning.core.host.viewmodel.EditServerIntent
import indi.pplong.composelearning.core.host.viewmodel.EditServerUiState
import indi.pplong.composelearning.core.util.ServerPortInfo

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
        containerColor = MaterialTheme.colorScheme.surface
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
@Preview
internal fun EditServerBottomSheetContent(
    uiState: EditServerUiState = EditServerUiState(),
    onIntent: (EditServerIntent) -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = stringResource(R.string.add_a_host),
            modifier = Modifier.padding()
        )

        OutlinedTextField(
            value = uiState.host.host,
            onValueChange = { onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(host = it))) },
            label = { Text(stringResource(R.string.host)) },
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        )

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
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        )
        Row {
            OutlinedTextField(
                value = uiState.host.user,
                onValueChange = { onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(user = it))) },
                label = { Text(stringResource(R.string.user)) },
                modifier = Modifier
                    .padding(12.dp)
                    .weight(0.7f),
                supportingText = {
                    Text("123")
                }
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
                    .padding(12.dp)
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
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(stringResource(R.string.test_again))
                        }
                    }

                }

                ConnectivityTestState.INITIAL ->
                    Button(
                        onClick = { onIntent(EditServerIntent.TestConnectivity) },
                        modifier = Modifier.padding(12.dp)
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
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(stringResource(R.string.next))
                        }
                    }
                }
            }
        }


    }
}

@Composable
@Preview
fun ConfigureHostInfo(
    uiState: EditServerUiState = EditServerUiState(),
    onIntent: (EditServerIntent) -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = "Customize",
        )

        Text(
            style = MaterialTheme.typography.titleMedium,
            text = "Pick an Icon",
        )

        LazyColumn { }

        OutlinedTextField(
            value = uiState.host.nickname,
            onValueChange = { onIntent(EditServerIntent.OnChangeHostInfo(uiState.host.copy(nickname = it))) },
            label = { Text(stringResource(R.string.nickname)) },
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        )

        Button(
            onClick = { onIntent(EditServerIntent.SaveHost) },
            modifier = Modifier.padding(12.dp)
        ) {
            Text("Save")
        }
    }
}