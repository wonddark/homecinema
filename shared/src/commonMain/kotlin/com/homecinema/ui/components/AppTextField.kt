package com.homecinema.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppTextField(
    value: String,
    onChange: (value: String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val defaultModifier = Modifier.padding(horizontal = 4.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.then(defaultModifier),
        singleLine = true,
        shape = Shapes().extraLarge,
    )
}
