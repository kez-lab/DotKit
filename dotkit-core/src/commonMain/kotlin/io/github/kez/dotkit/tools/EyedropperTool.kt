package io.github.kez.dotkit.tools

import io.github.kez.dotkit.canvas.CanvasState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand

/**
 * 스포이드 도구
 * 캔버스에서 색상을 선택합니다.
 *
 * 동작:
 * - 클릭한 픽셀의 색상을 가져옵니다
 * - 커맨드를 생성하지 않습니다 (캔버스를 변경하지 않음)
 * - UI 레이어에서 색상 변경 이벤트를 처리해야 합니다
 */
class EyedropperTool : Tool {
    override val name: String = "Eyedropper"

    /**
     * 선택된 색상을 저장하는 상태
     */
    data class EyedropperState(
        override val startPoint: Point,
        override val currentPoint: Point,
        override val color: Int,
        override val affectedPixels: List<Pair<Point, Int>> = emptyList(),
        val pickedColor: Int? = null  // 선택된 색상
    ) : ToolState

    override fun onDown(state: CanvasState, point: Point, color: Int): ToolState {
        val layer = state.activeLayer

        // 레이어가 없거나 범위를 벗어난 경우
        if (layer == null || !layer.isInBounds(point.x, point.y)) {
            return EyedropperState(
                startPoint = point,
                currentPoint = point,
                color = color,
                pickedColor = null
            )
        }

        // 합성된 캔버스에서 색상 가져오기 (모든 레이어 합성)
        val compositePixels = state.composite()
        val index = point.y * state.width + point.x
        val pickedColor = if (index in compositePixels.indices) {
            compositePixels[index]
        } else {
            null
        }

        return EyedropperState(
            startPoint = point,
            currentPoint = point,
            color = color,
            pickedColor = pickedColor
        )
    }

    override fun onMove(state: CanvasState, point: Point, color: Int, toolState: ToolState?): ToolState? {
        // 드래그 중에도 색상을 계속 업데이트
        val layer = state.activeLayer ?: return toolState

        if (!layer.isInBounds(point.x, point.y)) {
            return toolState
        }

        // 합성된 캔버스에서 색상 가져오기
        val compositePixels = state.composite()
        val index = point.y * state.width + point.x
        val pickedColor = if (index in compositePixels.indices) {
            compositePixels[index]
        } else {
            null
        }

        return EyedropperState(
            startPoint = (toolState as? EyedropperState)?.startPoint ?: point,
            currentPoint = point,
            color = color,
            pickedColor = pickedColor
        )
    }

    override fun onUp(state: CanvasState, point: Point, color: Int, toolState: ToolState?): CanvasCommand? {
        // 스포이드는 캔버스를 변경하지 않으므로 커맨드를 반환하지 않습니다
        // UI 레이어에서 toolState의 pickedColor를 사용하여 색상을 업데이트해야 합니다
        return null
    }
}
