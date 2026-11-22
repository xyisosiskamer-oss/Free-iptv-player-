package com.example.ui.components

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun isTelevisionDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.03f,
    focusedBorderColor: Color? = null,
    unfocusedBorderColor: Color = Color.Transparent,
    borderWidth: Dp = 2.5.dp
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val activeBorderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tv_focus_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) activeBorderColor else unfocusedBorderColor,
        label = "tv_focus_border"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            border = BorderStroke(
                width = if (isFocused) borderWidth else 1.dp,
                color = borderColor
            ),
            shape = shape
        )
        .onFocusChanged { state ->
            isFocused = state.isFocused
        }
        .focusable()
}
