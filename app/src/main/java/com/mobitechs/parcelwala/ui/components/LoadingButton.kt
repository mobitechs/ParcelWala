// ui/components/LoadingButton.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Button with loading state
 * Shows loading indicator when isLoading is true
 */
@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isOutlined: Boolean = false,
    containerColor: Color = Color(0xFF2196F3),
    contentColor: Color = Color.White
) {
    val buttonColors = if (isOutlined) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = containerColor
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    }

    val buttonContent: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (isOutlined) containerColor else contentColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
    }

    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled && !isLoading,
            colors = buttonColors,
            border = BorderStroke(1.dp, containerColor),
            shape = RoundedCornerShape(12.dp),
            content = buttonContent
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled && !isLoading,
            colors = buttonColors,
            shape = RoundedCornerShape(12.dp),
            content = buttonContent
        )
    }
}