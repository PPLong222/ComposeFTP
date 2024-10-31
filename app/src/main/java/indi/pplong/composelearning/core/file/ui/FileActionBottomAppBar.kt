package indi.pplong.composelearning.core.file.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.file.model.FileActionBottomAppBarStatus
import kotlinx.coroutines.delay


/**
 * Description:
 * @author PPLong
 * @date 10/30/24 10:31 AM
 */
enum class FileBottomAppBarAction {
    REFRESH,
    SEARCH,
    CREATE_FOLDER,

    // FIle
    DELETE,
    MOVE,
    SHARE,
    INFO
}

val fileBottomAppBarList = listOf(
    FileBottomAppBarAction.DELETE,
    FileBottomAppBarAction.MOVE,
    FileBottomAppBarAction.SHARE,
)


val directBottomAppBarList = listOf(
    FileBottomAppBarAction.REFRESH,
    FileBottomAppBarAction.SEARCH,
    FileBottomAppBarAction.CREATE_FOLDER,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun FileActionBottomAppBar(
    barStatus: FileActionBottomAppBarStatus = FileActionBottomAppBarStatus.DIRECTORY,
    modifier: Modifier = Modifier,
    onClickFAB: (FileActionBottomAppBarStatus) -> Unit = {},
    events: (FileBottomAppBarAction) -> Unit = {},
    scrollBehavior: BottomAppBarScrollBehavior? = null
) {
    val map = mapOf(
        FileBottomAppBarAction.REFRESH to rememberVectorPainter(Icons.Default.Refresh),
        FileBottomAppBarAction.SEARCH to rememberVectorPainter(Icons.Default.Search),
        FileBottomAppBarAction.CREATE_FOLDER to painterResource(R.drawable.ic_create_new_folder),
        FileBottomAppBarAction.DELETE to rememberVectorPainter(Icons.Default.Delete),
        FileBottomAppBarAction.MOVE to rememberVectorPainter(Icons.Default.KeyboardArrowDown),
        FileBottomAppBarAction.SHARE to rememberVectorPainter(Icons.Default.Share)

    )
    val list =
        if (barStatus == FileActionBottomAppBarStatus.DIRECTORY) directBottomAppBarList.toList() else fileBottomAppBarList.toList()

    BottomAppBar(
        actions = {
            Row {
                list.forEachIndexed { index, fileBottomAppBarAction ->
                    key(fileBottomAppBarAction) {
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 100L + 150L)
                            isVisible = true
                        }
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = (fadeIn(
                                animationSpec = tween(
                                    150
                                )
                            ) +
                                    slideInVertically(
                                        animationSpec = tween(
                                            150
                                        ),
                                        initialOffsetY = { it }
                                    )),
                            exit = fadeOut(animationSpec = tween(0, delayMillis = 0))
                        ) {
                            IconButton(
                                onClick = {
//                                    if (barStatus == FileActionBottomAppBarStatus.DIRECTORY) {
//                                        barStatus = FileActionBottomAppBarStatus.FILE
//                                    } else {
//                                        barStatus = FileActionBottomAppBarStatus.DIRECTORY
//                                    }
                                    events.invoke(fileBottomAppBarAction)
                                }
                            ) {
                                Icon(
                                    painter = map[fileBottomAppBarAction]!!,
                                    contentDescription = null
                                )

                            }

                        }
                    }
                }

            }


        },
        floatingActionButton = {
            // TODO: Optimization
            AnimatedContent(barStatus,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200, delayMillis = 150)) +
                            scaleIn(
                                initialScale = 0.2f,
                                animationSpec = tween(
                                    300,
                                    delayMillis = 150
                                )
                            ))
                        .togetherWith(fadeOut(animationSpec = tween(0)))
                }) { status ->
                when (status) {
                    FileActionBottomAppBarStatus.DIRECTORY -> {
                        FloatingActionButton(
                            shape = RoundedCornerShape(8.dp),
                            onClick = { onClickFAB(status) },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(
                                painter =
                                painterResource(R.drawable.ic_arrow_upward),
                                contentDescription = null
                            )
                        }
                    }

                    FileActionBottomAppBarStatus.FILE -> {
                        FloatingActionButton(
                            shape = RoundedCornerShape(8.dp),
                            onClick = { onClickFAB(status) },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(
                                painter =
                                painterResource(R.drawable.ic_download),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

        },
        scrollBehavior = scrollBehavior
    )
}
