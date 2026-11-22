package com.homecinema.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onChange: (value: String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    shape: Shape = ShapeDefaults.ExtraLarge,
) {
    val defaultModifier =
        Modifier
            .fillMaxWidth()
            .height(height = 44.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textColor = if (isFocused) Color(0xFFFFFFFF) else Color(0xE6FFFFFF)

    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.then(defaultModifier),
        interactionSource = interactionSource,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = textColor),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            interactionSource = interactionSource,
            visualTransformation = VisualTransformation.None,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.75f,
                        ),
                )
            },
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    shape = shape,
                    isError = false,
                    interactionSource = interactionSource,
                )
            },
        )
    }
}
