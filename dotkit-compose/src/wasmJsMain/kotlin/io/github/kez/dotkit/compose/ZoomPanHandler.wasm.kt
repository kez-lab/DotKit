package io.github.kez.dotkit.compose

import androidx.compose.ui.Modifier

/**
 * Web에서는 마우스 휠 줌을 브라우저 이벤트로 처리
 * (현재는 기본 구현 제공, 필요시 JS interop 사용 가능)
 */
actual fun Modifier.mouseWheelZoom(
    enabled: Boolean,
    onZoom: (Float) -> Unit
): Modifier = this
