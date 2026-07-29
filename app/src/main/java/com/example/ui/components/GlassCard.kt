package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.GlassBorder
import com.example.utils.bounceClick

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = DarkCardSurface.copy(alpha = 0.85f),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    gradientOverlay: Brush? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .then(
            if (gradientOverlay != null) {
                Modifier.background(gradientOverlay)
            } else {
                Modifier.background(backgroundColor)
            }
        )
        .border(BorderStroke(borderWidth, borderColor), shape)
        .then(
            if (onClick != null) {
                Modifier.bounceClick(onClick = onClick)
            } else Modifier
        )

    Box(
        modifier = cardModifier,
        content = content
    )
}
