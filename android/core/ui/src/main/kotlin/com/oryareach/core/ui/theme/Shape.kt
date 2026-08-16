package com.oryareach.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Derived from the web app's `--radius: 0.85rem` scale in `src/index.css`, converted at the
 * CSS convention of 1rem = 16px: sm = x0.6, md = x0.8, lg = x1, xl = x1.4.
 */
val OrYareachShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(11.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(19.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
