package indi.pplong.composelearning.core.base.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import indi.pplong.composelearning.R

/**
 * Description:
 * @author PPLong
 * @date 10/27/24 10:41 AM
 */

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    text: String = "",
    onValueChange: (String) -> Unit = {},
    labelString: String = ""
) {
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = onValueChange,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = modifier,
        label = { Text(labelString) },
        trailingIcon = {
            Box(modifier = Modifier.clickable {
                showPassword = showPassword.not()
            }) {
                Icon(
                    painter = painterResource(if (showPassword) R.drawable.ic_visibility else R.drawable.ic_visibility_off),
                    contentDescription = null,
                )
            }

        }
    )
}