package io.github.kez.dotkit.compose

import androidx.compose.ui.Modifier

/**
 * iOS에서는 마우스 휠 줌이 필요 없음 (터치 사용)
 */
actual fun Modifier.mouseWheelZoom(
    enabled: Boolean,
    onZoom: (Float) -> Unit
): Modifier = this
