package io.github.kez.dotkit.compose

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 줌/패닝 제스처 핸들러
 *
 * 2개 손가락 드래그와 핀치 줌을 처리합니다.
 *
 * @param onZoom 줌 콜백
 * @param onPan 패닝 콜백
 */
fun Modifier.zoomPanGestures(
    enabled: Boolean = true,
    onZoom: (Float) -> Unit,
    onPan: (Offset) -> Unit
): Modifier {
    if (!enabled) return this

    return this.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            // 줌 처리 (핀치 제스처)
            if (zoom != 1f) {
                onZoom(zoom)
            }

            // 패닝 처리 (2개 손가락 드래그)
            if (pan != Offset.Zero) {
                onPan(pan)
            }
        }
    }
}

/**
 * 마우스 휠 줌 핸들러
 *
 * 데스크톱에서 마우스 휠로 줌을 처리합니다.
 * (플랫폼별 구현 필요)
 */
expect fun Modifier.mouseWheelZoom(
    enabled: Boolean = true,
    onZoom: (Float) -> Unit
): Modifier
