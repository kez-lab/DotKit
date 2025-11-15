package io.github.kez.dotkit.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

/**
 * 데스크톱에서 마우스 휠로 줌 처리
 */
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.mouseWheelZoom(
    enabled: Boolean,
    onZoom: (Float) -> Unit
): Modifier {
    if (!enabled) return this

    return this.onPointerEvent(PointerEventType.Scroll) { event ->
        val scrollDelta = event.changes.first().scrollDelta.y

        // 스크롤 방향에 따라 줌 인/아웃
        val zoomFactor = if (scrollDelta < 0) {
            1.1f // 줌 인
        } else {
            0.9f // 줌 아웃
        }

        onZoom(zoomFactor)
    }
}
