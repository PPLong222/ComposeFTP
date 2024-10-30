package indi.pplong.composelearning.core.load.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import indi.pplong.composelearning.R

/**
 * Description:
 * @author PPLong
 * @date 10/25/24 1:44 PM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TwinTab(
    modifier: Modifier = Modifier,
    selectIndex: Int = 0,
    activatedColor: Color = MaterialTheme.colorScheme.surfaceDim,
    deactivatedColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onTabClick: (Int) -> Unit = {}
) {
    val options = listOf(stringResource(R.string.download), stringResource(R.string.upload))
    val iconList = listOf(R.drawable.ic_download, R.drawable.ic_upload)

    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, title ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = {
                        if (selectIndex != index) {
                            onTabClick(index)
                        }
                    },
                    selected = index == selectIndex,
                    icon = {
                        Icon(
                            painter = painterResource(iconList[index]),
                            contentDescription = null
                        )
                    }
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}