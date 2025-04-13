package indi.pplong.composelearning.core.host.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import indi.pplong.composelearning.R
import indi.pplong.composelearning.core.host.model.ServerConnectionStatus
import indi.pplong.composelearning.core.host.model.ServerItemInfo
import indi.pplong.composelearning.core.host.viewmodel.ServerUiIntent
import indi.pplong.composelearning.core.host.viewmodel.ServerUiState
import indi.pplong.composelearning.sys.ui.theme.ComposeLearningTheme
import kotlin.math.absoluteValue

/**
 * Description:
 * @author PPLong
 * @date 9/26/24 7:05 PM
 */

@Composable
fun ConnectedHost(
    serverItemInfo: ServerItemInfo = ServerItemInfo(
        host = "192.168.1.1",
        nickname = "Nickname",
        isSFTP = true
    ),
    onIntent: (ServerUiIntent) -> Unit = {},
    isConnecting: Boolean = false
) {

    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(imageVector = Icons.Default.Share, null)
            },
            headlineContent = {
                Text(serverItemInfo.nickname, style = MaterialTheme.typography.titleMedium)
            },
            overlineContent = {},
            supportingContent = {
                Column {
                    Text(
                        "User: ${serverItemInfo.user}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (serverItemInfo.isSFTP) {
                        Icon(
                            painter = painterResource(R.drawable.ic_security),
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp),
                        )
                    }
                    if (!isConnecting) {
                        Button(
                            onClick = {
                                onIntent(ServerUiIntent.ConnectServer(serverItemInfo))
                            },
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .width(48.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, null)
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }

            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        )
    }
}

@Preview
@Composable
fun ConnectedHostPreview() {
    ComposeLearningTheme {
        ConnectedHost()
    }
}

@Composable
@Preview
fun HostCard(
    modifier: Modifier = Modifier,
    serverItemInfo: ServerItemInfo = ServerItemInfo(
        host = "192.168.1.1",
        nickname = "Yunlong",
        isSFTP = true,
        user = "root"
    ),
    onIntent: (ServerUiIntent) -> Unit = {},
) {
    Card {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RotatedDashedCircle(
                modifier = Modifier.size(150.dp),
                radius = 60.dp,
                color = MaterialTheme.colorScheme.primary,
                sweepAngle = 30F,
                strokeWidth = 8.dp,
                gapAngle = 15F,
                innerColor = MaterialTheme.colorScheme.primary,
                innerRadius = 30.dp
            )

            Text(
                text = serverItemInfo.nickname,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(top = 30.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Host: ${serverItemInfo.host}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_visibility),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "User: ${serverItemInfo.user}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_visibility),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (serverItemInfo.isSFTP) "SFTP Secured" else "Tradition FTP",
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF409343)
                )
                Icon(
                    painter = painterResource(if (serverItemInfo.isSFTP) R.drawable.ic_security else R.drawable.ic_security_off),
                    contentDescription = null,
                    tint = if (serverItemInfo.isSFTP) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                )

            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onIntent(ServerUiIntent.ConnectServer(serverItemInfo))
                },
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                if (serverItemInfo.connectedStatus == ServerConnectionStatus.INITIAL) {
                    Text("Connect", style = MaterialTheme.typography.titleMedium)
                } else if (serverItemInfo.connectedStatus == ServerConnectionStatus.CONNECTING
                    || serverItemInfo.connectedStatus == ServerConnectionStatus.CONNECTED
                ) {
                    Text("Connecting", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
@Preview
fun PreviewHostCard(
    modifier: Modifier = Modifier,
    serverItemInfo: ServerItemInfo = ServerItemInfo(
        host = "192.168.1.1",
        nickname = "Yunlong",
        isSFTP = false,
        user = "root"
    ),
    onIntent: (ServerUiIntent) -> Unit = {},
) {
    HostCard(modifier, serverItemInfo, onIntent)
}

@Preview
@Composable
fun PreviewRotatedDashedCircle() {
    RotatedDashedCircle(
        modifier = Modifier.size(150.dp),
        radius = 60.dp,
        color = MaterialTheme.colorScheme.primary,
        sweepAngle = 30F,
        strokeWidth = 8.dp,
        gapAngle = 15F,
        innerColor = MaterialTheme.colorScheme.primary,
        innerRadius = 30.dp
    )
}

@Composable
fun RotatedDashedCircle(
    modifier: Modifier = Modifier,
    radius: Dp,
    color: Color,
    sweepAngle: Float,
    strokeWidth: Dp,
    gapAngle: Float,
    innerRadius: Dp,
    innerColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
    )

    DashedCircle(
        modifier.rotate(angle), radius, color, sweepAngle, strokeWidth, gapAngle,
        innerRadius, innerColor
    )
}

@Composable
fun DashedCircle(
    modifier: Modifier = Modifier,
    radius: Dp,
    color: Color, sweepAngle: Float, strokeWidth: Dp, gapAngle: Float,
    innerRadius: Dp,
    innerColor: Color
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = 0F
        drawCircle(color = innerColor, radius = innerRadius.toPx(), center = center)
        while (startAngle < 360F) {
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius.toPx(), center.y - radius.toPx()),
                size = Size(radius.toPx() * 2, radius.toPx() * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += sweepAngle + gapAngle
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun StackedHostCardPager(
    modifier: Modifier = Modifier,
    uiState: ServerUiState = ServerUiState(),
    onIntent: (ServerUiIntent) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { uiState.serverList.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->

        val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).coerceIn(-1f, 1f) // 限制偏移量范围

        val scale = lerp(0.60f, 1f, 1f - pageOffset.absoluteValue)
        val translationX = lerp(40f, 0f, 1f - pageOffset.absoluteValue)
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.translationX = translationX * pageOffset
                    this.alpha = lerp(0.5f, 1f, 1f - pageOffset.absoluteValue)
                }
        ) {
            HostCard(serverItemInfo = uiState.serverList[page], onIntent = onIntent)
        }
    }
}