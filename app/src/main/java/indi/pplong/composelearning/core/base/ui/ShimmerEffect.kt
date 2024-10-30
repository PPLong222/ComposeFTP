package indi.pplong.composelearning.core.base.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Description:
 * @author PPLong
 * @date 10/26/24 6:11 PM
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember {
        mutableStateOf(IntSize.Zero)
    }

    val transition = rememberInfiniteTransition(label = "")
    val offsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        ), label = ""
    )

    val shimmerBrush by rememberUpdatedState(
        Brush.linearGradient(
            listOf(
                Color(0xFFC0BCBC),
                Color(0xFF878491),
                Color(0xFFC0BCBC)
            ),
            start = Offset(offsetX, 0f),
            end = Offset(offsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
    background(brush = shimmerBrush)
        .onGloballyPositioned {
            if (size != it.size) {
                size = it.size
            }
        }
        .clipToBounds()
}

@Preview
@Composable
fun PreviewListShimmerEffect(modifier: Modifier = Modifier) {
    Column {
        repeat(10) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .shimmerEffect()
            )
        }
    }

}